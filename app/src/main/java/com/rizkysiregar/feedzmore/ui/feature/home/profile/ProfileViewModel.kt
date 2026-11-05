package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.session.SessionManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.feedzmore.domain.profile.FetchUserProfileUseCase
import com.rizkysiregar.feedzmore.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

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
	private val getFollowStatusUseCase: com.rizkysiregar.feedzmore.domain.user.GetFollowStatusUseCase
) : BaseViewModel(dataStore) {

	// clear user data aka logout
	fun clearDataStore() {
		sessionManager.logout()
	}

	private val _profileState = MutableStateFlow<ResultState<UserSearchItemResponse>?>(null)
	val profileState: StateFlow<ResultState<UserSearchItemResponse>?> = _profileState

	private val _followStatus = MutableStateFlow<ResultState<com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse>?>(null)
	val followStatus: StateFlow<ResultState<com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse>?> = _followStatus

	private var fetchJob: Job? = null

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
				_profileState.value = ResultState.Failure("User ID not found")
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
