package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.UserProfileResponse
import com.rizkysiregar.feedzmore.domain.profile.FetchUserProfileUseCase
import com.rizkysiregar.feedzmore.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
	private val fetchUserProfileUseCase: FetchUserProfileUseCase
) : BaseViewModel(dataStore) {

	// clear user data aka logout
	fun clearDataStore() {
		viewModelScope.launch {
			dataStore.clearAll()
		}
	}

	private val _profileState = MutableStateFlow<ResultState<UserProfileResponse>?>(null)
	val profileState: StateFlow<ResultState<UserProfileResponse>?> = _profileState

	fun fetchUserProfile() {
		viewModelScope.launch {
			_profileState.value = ResultState.Loading

			val userId = dataStore.get(PrefKeys.userId, "").first()

			if (userId.isEmpty()) {
				_profileState.value = ResultState.Failure("User ID not found")
				return@launch
			}

			fetchUserProfileUseCase(userId)
				.collect { resultState ->
					_profileState.value = resultState
				}
		}
	}


}