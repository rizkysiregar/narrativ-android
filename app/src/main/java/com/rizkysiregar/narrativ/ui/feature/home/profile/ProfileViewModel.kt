package com.rizkysiregar.narrativ.ui.feature.home.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.session.SessionManager
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.narrativ.data.remote.upload.UploadHttpException
import com.rizkysiregar.narrativ.domain.profile.FetchUserProfileUseCase
import com.rizkysiregar.narrativ.domain.profile.ProfilePhotoUseCase
import com.rizkysiregar.narrativ.media.MediaValidationException
import com.rizkysiregar.narrativ.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import android.net.Uri
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.rizkysiregar.narrativ.i18n.AppStrings
import com.rizkysiregar.narrativ.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */


@HiltViewModel
class ProfileViewModel @Inject constructor(
	private val dataStore: DataStoreManager,
	private val sessionManager: SessionManager,
	private val fetchUserProfileUseCase: FetchUserProfileUseCase,
	private val getFollowStatusUseCase: com.rizkysiregar.narrativ.domain.user.GetFollowStatusUseCase,
	private val profilePhotoUseCase: ProfilePhotoUseCase,
	private val appStrings: AppStrings
) : BaseViewModel(dataStore) {

	// clear user data aka logout
	fun clearDataStore() {
		sessionManager.logout()
	}

	private val _profileState = MutableStateFlow<ResultState<UserSearchItemResponse>?>(null)
	val profileState: StateFlow<ResultState<UserSearchItemResponse>?> = _profileState

	private val _followStatus = MutableStateFlow<ResultState<com.rizkysiregar.narrativ.data.remote.model.response.user.FollowStatusResponse>?>(null)
	val followStatus: StateFlow<ResultState<com.rizkysiregar.narrativ.data.remote.model.response.user.FollowStatusResponse>?> = _followStatus

	private var fetchJob: Job? = null

	private val _isUpdatingPhoto = MutableStateFlow(false)
	val isUpdatingPhoto: StateFlow<Boolean> = _isUpdatingPhoto

	// one-shot messages (success or failure) for a snackbar
	private val _photoMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
	val photoMessages: SharedFlow<String> = _photoMessages.asSharedFlow()

	/** Uploads the file produced by the crop step and deletes it afterwards. */
	fun onPhotoCropped(file: File) {
		updatePhoto(appStrings.get(R.string.profile_photo_updated)) {
			try {
				profilePhotoUseCase.upload(Uri.fromFile(file), "image/jpeg")
			} finally {
				file.delete()
			}
		}
	}

	fun removePhoto() {
		updatePhoto(appStrings.get(R.string.profile_photo_removed)) { profilePhotoUseCase.remove() }
	}

	private fun updatePhoto(successMessage: String, action: suspend () -> UserSearchItemResponse) {
		if (_isUpdatingPhoto.value) return
		viewModelScope.launch {
			_isUpdatingPhoto.value = true
			try {
				_profileState.value = ResultState.Success(action())
				_photoMessages.tryEmit(successMessage)
			} catch (e: CancellationException) {
				throw e
			} catch (e: MediaValidationException) {
				_photoMessages.tryEmit(appStrings.get(e.messageRes, *e.args.toTypedArray()))
			} catch (e: UploadHttpException) {
				_photoMessages.tryEmit(appStrings.get(R.string.profile_err_upload))
			} catch (e: Exception) {
				_photoMessages.tryEmit(appStrings.get(R.string.profile_err_update))
			} finally {
				_isUpdatingPhoto.value = false
			}
		}
	}

	/**
	 * Loads profile and follow status in parallel. A newer call cancels the previous one so a
	 * stale response can't overwrite fresh data. While refreshing, the already-loaded profile
	 * stays on screen instead of falling back to a full-screen spinner.
	 */
	fun fetchUserProfile() {
		fetchJob?.cancel()
		fetchJob = viewModelScope.launch {
			val hasProfile = _profileState.value is ResultState.Success
			if (!hasProfile) _profileState.value = ResultState.Loading

			val userId = dataStore.get(PrefKeys.userId, "").first()

			if (userId.isEmpty()) {
				_profileState.value = ResultState.Failure(appStrings.get(R.string.error_user_id_missing))
				return@launch
			}

			launch {
				fetchUserProfileUseCase(userId).collect { result ->
					// Keep showing the old profile if a refresh fails.
					if (hasProfile && result !is ResultState.Success) return@collect
					_profileState.value = result
				}
			}
			launch {
				getFollowStatusUseCase(userId).collect { status ->
					if (_followStatus.value is ResultState.Success && status !is ResultState.Success) return@collect
					_followStatus.value = status
				}
			}
		}
	}

}
