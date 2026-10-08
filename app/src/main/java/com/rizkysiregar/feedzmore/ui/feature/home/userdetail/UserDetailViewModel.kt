package com.rizkysiregar.feedzmore.ui.feature.home.userdetail

import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.feedzmore.domain.profile.FetchUserProfileUseCase
import com.rizkysiregar.feedzmore.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 * Description: Software Engineer
 */
@HiltViewModel
class UserDetailViewModel @Inject constructor(
	dataStore: DataStoreManager,
	private val fetchUserProfileUseCase: FetchUserProfileUseCase,
	private val getFollowStatusUseCase: com.rizkysiregar.feedzmore.domain.user.GetFollowStatusUseCase,
	private val followUserUseCase: com.rizkysiregar.feedzmore.domain.user.FollowUserUseCase,
	private val unfollowUserUseCase: com.rizkysiregar.feedzmore.domain.user.UnfollowUserUseCase
) : BaseViewModel(dataStore) {

	private val _profileState = MutableStateFlow<ResultState<UserSearchItemResponse>?>(null)
	val profileState: StateFlow<ResultState<UserSearchItemResponse>?> = _profileState

	private val _followStatus = MutableStateFlow<ResultState<com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse>?>(null)
	val followStatus: StateFlow<ResultState<com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse>?> = _followStatus

	private val _isFollowActionLoading = MutableStateFlow(false)
	val isFollowActionLoading: StateFlow<Boolean> = _isFollowActionLoading

	fun loadFollowStatus(userId: String) {
		viewModelScope.launch {
			getFollowStatusUseCase(userId).collect { _followStatus.value = it }
		}
	}

	fun toggleFollow(userId: String) {
		val current = _followStatus.value
		if (current !is ResultState.Success || _isFollowActionLoading.value) return
		viewModelScope.launch {
			_isFollowActionLoading.value = true
			val flow = if (current.data.isFollowing) unfollowUserUseCase(userId) else followUserUseCase(userId)
			flow.collect { result ->
				if (result is ResultState.Success) {
					_followStatus.value = ResultState.Success(
						current.data.copy(
							isFollowing = !current.data.isFollowing,
							followerCount = if (current.data.isFollowing) current.data.followerCount - 1 else current.data.followerCount + 1
						)
					)
					_isFollowActionLoading.value = false
				} else if (result is ResultState.Failure) {
					_isFollowActionLoading.value = false
				}
			}
		}
	}

	fun loadUser(userId: String) {
		viewModelScope.launch {
			_profileState.value = ResultState.Loading

			fetchUserProfileUseCase(userId).collect { resultState ->
				_profileState.value = resultState
			}
		}
	}
}
