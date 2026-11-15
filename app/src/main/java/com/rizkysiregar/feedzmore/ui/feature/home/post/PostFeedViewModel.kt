package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.domain.post.MediaPreparationUseCase
import com.rizkysiregar.feedzmore.domain.profile.FetchUserProfileUseCase
import com.rizkysiregar.feedzmore.domain.post.PickedMedia
import com.rizkysiregar.feedzmore.domain.post.UploadManager
import com.rizkysiregar.feedzmore.media.MediaValidationException
import com.rizkysiregar.feedzmore.telemetry.AnalyticsEvents
import com.rizkysiregar.feedzmore.telemetry.AppAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.rizkysiregar.feedzmore.i18n.AppStrings
import com.rizkysiregar.feedzmore.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 06/12/25
 * Description: Software Engineer
 */

@HiltViewModel
class PostFeedViewModel @Inject constructor(
	private val app: Application,
	private val mediaPreparationUseCase: MediaPreparationUseCase,
	private val uploadManager: UploadManager,
	private val dataStore: DataStoreManager,
	private val fetchUserProfileUseCase: FetchUserProfileUseCase,
	private val analytics: AppAnalytics
) : ViewModel() {

	// UI State for watching property changes
	private val _uiState = MutableStateFlow(PostFeedUiState())
	val uiState = _uiState.asStateFlow()

	val isPreparing: StateFlow<Boolean> = mediaPreparationUseCase.isPreparing

	init {
		loadAuthor()
	}

	private val _navigateHome = MutableSharedFlow<Unit>()
	val navigateHome: SharedFlow<Unit> = _navigateHome.asSharedFlow()

	private fun loadAuthor() {
		viewModelScope.launch {
			val userId = dataStore.get(PrefKeys.userId, "").first()
			if (userId.isBlank()) return@launch
			fetchUserProfileUseCase(userId).collect { result ->
				if (result is ResultState.Success) {
					_uiState.update { it.copy(author = result.data) }
				}
			}
		}
	}

	/** Adds the picked media (up to [MAX_POST_MEDIA] in total); the ones that do not fit or are invalid are skipped. */
	fun onMediaPicked(uris: List<Uri>) {
		Log.i("PostFeedViewModel", "## onMediaPicked() called with ${uris.size} media")
		var error: String? = null
		val added = mutableListOf<PickedMedia>()
		for (uri in uris) {
			val current = _uiState.value.selectedMedia + added
			if (current.any { it.uri == uri }) continue
			if (current.size >= MAX_POST_MEDIA) {
				error = strings.getString(R.string.post_err_max_media, MAX_POST_MEDIA)
				break
			}
			val mimeType = app.contentResolver.getType(uri)
			val invalid = validateMedia(uri, mimeType)
			if (invalid != null) {
				error = invalid
			} else {
				added += PickedMedia(uri, mimeType)
			}
		}
		_uiState.update {
			it.copy(
				selectedMedia = it.selectedMedia + added,
				validationError = error
			)
		}
	}

	fun onMediaRemoved(index: Int) {
		_uiState.update { state ->
			if (index !in state.selectedMedia.indices) return@update state
			val remaining = state.selectedMedia.filterIndexed { i, _ -> i != index }
			state.copy(
				selectedMedia = remaining,
				validationError = null
			)
		}
	}

	/**
	 * Prepares the media (compress/copy) on this screen, then hands the upload
	 * off to the background worker and signals the UI to return home.
	 */
	fun submitPost() {
		val state = _uiState.value
		Log.i("PostFlow", "submitPost: canSubmit=${state.canSubmit} media=${state.selectedMedia.size} captionLength=${state.caption.length}")
		if (!state.canSubmit) return

		viewModelScope.launch {
			try {
				val prepared = mediaPreparationUseCase.prepareAll(state.selectedMedia)
				uploadManager.enqueue(media = prepared, caption = state.caption)
				analytics.log(
					AnalyticsEvents.POST_SUBMITTED,
					AnalyticsEvents.Param.MEDIA_COUNT to state.selectedMedia.size,
					AnalyticsEvents.Param.HAS_VIDEO to state.selectedMedia.any { it.mimeType?.startsWith("video/") == true },
					AnalyticsEvents.Param.HAS_CAPTION to state.caption.isNotBlank()
				)
				Log.i("PostFlow", "submitPost: enqueued, going home")
				_navigateHome.emit(Unit)
			} catch (e: MediaValidationException) {
				Log.w("PostFlow", "submitPost: media rejected: ${e.message}")
				_uiState.update { it.copy(validationError = strings.getString(e.messageRes, *e.args.toTypedArray())) }
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				Log.e("PostFlow", "submitPost failed", e)
				_uiState.update { it.copy(validationError = strings.getString(R.string.post_err_prepare)) }
			}
		}
	}

	private val strings get() = AppStrings.localized(app)

	private fun validateMedia(uri: Uri, mimeType: String?): String? {
		if (mimeType == null) return strings.getString(R.string.media_err_unknown_type)
		val sizeBytes = runCatching {
			app.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
		}.getOrNull()
		return when {
			mimeType.startsWith("image/") -> {
				when {
					mimeType !in ALLOWED_IMAGE_TYPES -> strings.getString(R.string.media_err_image_format)
					// Static images are compressed before upload; GIFs are sent as-is.
					mimeType == "image/gif" && sizeBytes != null && sizeBytes > 5L * 1024 * 1024 ->
						strings.getString(R.string.media_err_gif_size)
					else -> null
				}
			}
			mimeType in ALLOWED_VIDEO_TYPES -> {
				if (sizeBytes != null && sizeBytes > 50L * 1024 * 1024) return strings.getString(R.string.media_err_video_50)
				val durationMs = runCatching {
					val retriever = android.media.MediaMetadataRetriever()
					app.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
						retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
					}
					val d = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
					retriever.release()
					d?.toLongOrNull()
				}.getOrNull()
				if (durationMs != null && durationMs > 60_000L) strings.getString(R.string.media_err_duration_max)
				else null
			}
			else -> strings.getString(R.string.media_err_format)
		}
	}

	fun clearValidationError() {
		_uiState.update { it.copy(validationError = null) }
	}

	fun onCaptionChanged(caption: String) {
		Log.i("PostFeedViewModel", "## onCaptionChanged() called")
		_uiState.update {
			it.copy(caption = caption.take(MAX_CAPTION_LENGTH))
		}
	}

	private companion object {
		val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
		val ALLOWED_VIDEO_TYPES = setOf("video/mp4", "video/quicktime", "video/webm")
	}
}
