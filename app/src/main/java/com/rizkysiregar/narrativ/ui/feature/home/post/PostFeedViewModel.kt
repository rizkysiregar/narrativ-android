package com.rizkysiregar.narrativ.ui.feature.home.post

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.domain.post.MediaPreparationUseCase
import com.rizkysiregar.narrativ.domain.post.MediaValidator
import com.rizkysiregar.narrativ.domain.profile.FetchUserProfileUseCase
import com.rizkysiregar.narrativ.domain.post.PickedMedia
import com.rizkysiregar.narrativ.domain.post.UploadManager
import com.rizkysiregar.narrativ.media.MediaValidationException
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.telemetry.AppAnalytics
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
import com.rizkysiregar.narrativ.i18n.AppStrings
import com.rizkysiregar.narrativ.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 06/12/25
 * Description: Software Engineer
 */

@HiltViewModel
class PostFeedViewModel @Inject constructor(
	private val mediaValidator: MediaValidator,
	private val mediaPreparationUseCase: MediaPreparationUseCase,
	private val uploadManager: UploadManager,
	private val dataStore: DataStoreManager,
	private val fetchUserProfileUseCase: FetchUserProfileUseCase,
	private val analytics: AppAnalytics,
	private val strings: AppStrings
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
				error = strings.get(R.string.post_err_max_media, MAX_POST_MEDIA)
				break
			}
			val check = mediaValidator.check(uri)
			if (check.errorRes != null) {
				error = strings.get(check.errorRes)
			} else {
				added += PickedMedia(uri, check.mimeType)
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
				_uiState.update { it.copy(validationError = strings.get(e.messageRes, *e.args.toTypedArray())) }
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				Log.e("PostFlow", "submitPost failed", e)
				_uiState.update { it.copy(validationError = strings.get(R.string.post_err_prepare)) }
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
}
