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
	private val fetchUserProfileUseCase: FetchUserProfileUseCase
) : BaseViewModel(dataStore) {

	private val _profileState = MutableStateFlow<ResultState<UserSearchItemResponse>?>(null)
	val profileState: StateFlow<ResultState<UserSearchItemResponse>?> = _profileState

	fun loadUser(userId: String) {
		viewModelScope.launch {
			_profileState.value = ResultState.Loading

			fetchUserProfileUseCase(userId).collect { resultState ->
				_profileState.value = resultState
			}
		}
	}
}
