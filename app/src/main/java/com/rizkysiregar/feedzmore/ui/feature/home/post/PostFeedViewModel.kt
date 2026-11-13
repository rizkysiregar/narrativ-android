package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
	private val fetchUserProfileUseCase: FetchUserProfileUseCase
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
				error = "You can add up to $MAX_POST_MEDIA photos or videos per post"
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

	/** Tapping a recent photo selects it, tapping it again takes it out of the post. */
	fun onRecentImageToggled(uri: Uri) {
		val index = _uiState.value.selectedMedia.indexOfFirst { it.uri == uri }
		if (index >= 0) onMediaRemoved(index) else onMediaPicked(listOf(uri))
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
				Log.i("PostFlow", "submitPost: enqueued, going home")
				_navigateHome.emit(Unit)
			} catch (e: MediaValidationException) {
				Log.w("PostFlow", "submitPost: media rejected: ${e.message}")
				_uiState.update { it.copy(validationError = e.message ?: "Invalid media") }
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				Log.e("PostFlow", "submitPost failed", e)
				_uiState.update { it.copy(validationError = "Could not prepare the media, please try again") }
			}
		}
	}

	private fun validateMedia(uri: Uri, mimeType: String?): String? {
		if (mimeType == null) return "Unknown media type"
		val sizeBytes = runCatching {
			app.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
		}.getOrNull()
		return when {
			mimeType.startsWith("image/") -> {
				when {
					mimeType !in ALLOWED_IMAGE_TYPES -> "Unsupported image format"
					// Static images are compressed before upload; GIFs are sent as-is.
					mimeType == "image/gif" && sizeBytes != null && sizeBytes > 5L * 1024 * 1024 ->
						"GIFs can be up to 5MB"
					else -> null
				}
			}
			mimeType in ALLOWED_VIDEO_TYPES -> {
				if (sizeBytes != null && sizeBytes > 50L * 1024 * 1024) return "Videos can be up to 50MB"
				val durationMs = runCatching {
					val retriever = android.media.MediaMetadataRetriever()
					app.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
						retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
					}
					val d = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
					retriever.release()
					d?.toLongOrNull()
				}.getOrNull()
				if (durationMs != null && durationMs > 60_000L) "Videos can be up to 60 seconds"
				else null
			}
			else -> "Unsupported media format. Use an image or an mp4/mov/webm video"
		}
	}

	fun loadRecentImages() {
		if (_uiState.value.isLoadingGallery) return

		viewModelScope.launch(Dispatchers.IO) {
			_uiState.update { it.copy(isLoadingGallery = true) }

			val images = runCatching {
				val projection = arrayOf(MediaStore.Images.Media._ID)
				app.contentResolver.query(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					projection,
					null,
					null,
					"${MediaStore.Images.Media.DATE_ADDED} DESC"
				)?.use { cursor ->
					val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
					buildList {
						while (cursor.moveToNext() && size < RECENT_IMAGE_LIMIT) {
							add(
								ContentUris.withAppendedId(
									MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
									cursor.getLong(idColumn)
								)
							)
						}
					}
				} ?: emptyList()
			}.getOrDefault(emptyList())

			_uiState.update { state ->
				// nothing is pre-selected: a post may be caption only
				state.copy(recentImages = images, isLoadingGallery = false)
			}
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
		const val RECENT_IMAGE_LIMIT = 18
		val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
		val ALLOWED_VIDEO_TYPES = setOf("video/mp4", "video/quicktime", "video/webm")
	}
}
