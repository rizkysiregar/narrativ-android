package com.rizkysiregar.narrativ.ui.feature.home.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.narrativ.domain.user.GetFollowersUseCase
import com.rizkysiregar.narrativ.domain.user.GetFollowingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FollowListViewModel @Inject constructor(
	private val getFollowersUseCase: GetFollowersUseCase,
	private val getFollowingUseCase: GetFollowingUseCase
) : ViewModel() {

	private val _items = MutableStateFlow<List<FollowUserResponse>>(emptyList())
	val items: StateFlow<List<FollowUserResponse>> = _items

	private val _isLoading = MutableStateFlow(false)
	val isLoading: StateFlow<Boolean> = _isLoading

	fun load(userId: String, type: String) {
		viewModelScope.launch {
			_isLoading.value = true
			val flow = if (type == "followers") getFollowersUseCase(userId, 0, 50)
			else getFollowingUseCase(userId, 0, 50)
			flow.collect { result ->
				if (result is ResultState.Success) {
					_items.value = result.data.content
					_isLoading.value = false
				} else if (result is ResultState.Failure) {
					_isLoading.value = false
				}
			}
		}
	}
}
