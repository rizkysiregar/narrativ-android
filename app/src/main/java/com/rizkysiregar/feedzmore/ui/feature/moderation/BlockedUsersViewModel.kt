package com.rizkysiregar.feedzmore.ui.feature.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.feedzmore.data.repository.ModerationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedUsersUiState(
	val users: List<FollowUserResponse> = emptyList(),
	val isLoading: Boolean = true,
	val loadFailed: Boolean = false,
	/** Users whose unblock is in flight; their button waits. */
	val unblocking: Set<String> = emptySet()
)

@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
	private val moderationRepository: ModerationRepository
) : ViewModel() {

	private val _uiState = MutableStateFlow(BlockedUsersUiState())
	val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

	init {
		load()
	}

	fun load() {
		_uiState.update { it.copy(isLoading = true, loadFailed = false) }
		viewModelScope.launch {
			val users = mutableListOf<FollowUserResponse>()
			var page = 0
			// a block list is short, so it is read whole
			while (page < MAX_PAGES) {
				var last = true
				var failed = false
				moderationRepository.getBlockedUsers(page, PAGE_SIZE).collect { result ->
					when (result) {
						is ResultState.Success -> {
							users += result.data.content
							last = result.data.last || result.data.content.isEmpty()
						}
						is ResultState.Failure -> failed = true
						else -> Unit
					}
				}
				if (failed) {
					_uiState.update { it.copy(isLoading = false, loadFailed = true) }
					return@launch
				}
				if (last) break
				page++
			}
			_uiState.update { it.copy(users = users, isLoading = false, loadFailed = false) }
		}
	}

	fun unblock(user: FollowUserResponse) {
		if (user.id in _uiState.value.unblocking) return
		_uiState.update { it.copy(unblocking = it.unblocking + user.id) }
		viewModelScope.launch {
			var done = false
			moderationRepository.unblockUser(user.id).collect { result ->
				if (result is ResultState.Success) done = true
			}
			_uiState.update { state ->
				state.copy(
					users = if (done) state.users.filterNot { it.id == user.id } else state.users,
					unblocking = state.unblocking - user.id
				)
			}
		}
	}

	private companion object {
		const val PAGE_SIZE = 50
		const val MAX_PAGES = 20
	}
}
