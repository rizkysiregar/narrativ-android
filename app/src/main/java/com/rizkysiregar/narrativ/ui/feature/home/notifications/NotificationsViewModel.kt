package com.rizkysiregar.narrativ.ui.feature.home.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.notification.NotificationItem
import com.rizkysiregar.narrativ.data.repository.NotificationRepository
import com.rizkysiregar.narrativ.domain.notification.UnreadNotifications
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
	val items: List<NotificationItem> = emptyList(),
	val isLoading: Boolean = false,
	val isRefreshing: Boolean = false,
	val errorMessage: String? = null,
	val nextCursor: String? = null,
	val hasLoadedFirstPage: Boolean = false
) {
	val hasMore: Boolean get() = nextCursor != null
}

/**
 * The inbox. Opening it marks everything read on the server and clears the badge, while the entries that were
 * unread keep their mark here until the user leaves, so they can still tell what is new.
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
	private val repository: NotificationRepository,
	private val unreadNotifications: UnreadNotifications
) : ViewModel() {

	private val _uiState = MutableStateFlow(NotificationsUiState())
	val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

	private var loadJob: Job? = null

	init {
		load(refresh = false)
		markAllRead()
	}

	fun refresh() = load(refresh = true)

	fun loadMore() {
		val state = _uiState.value
		if (state.isLoading || !state.hasMore || state.errorMessage != null) return
		load(refresh = false, cursor = state.nextCursor)
	}

	fun retry() {
		val state = _uiState.value
		_uiState.update { it.copy(errorMessage = null) }
		if (state.hasLoadedFirstPage) loadMore() else load(refresh = false)
	}

	/** The user opened [item]: it is not new any more. */
	fun onOpened(item: NotificationItem) {
		if (!item.unread) return
		_uiState.update { state ->
			state.copy(items = state.items.map { if (it.groupKey == item.groupKey) it.copy(unread = false) else it })
		}
	}

	private fun load(refresh: Boolean, cursor: String? = null) {
		if (refresh) loadJob?.cancel() else if (loadJob?.isActive == true) return
		_uiState.update { it.copy(isLoading = true, isRefreshing = refresh, errorMessage = null) }
		loadJob = viewModelScope.launch {
			repository.getNotifications(cursor, PAGE_SIZE).collect { result ->
				when (result) {
					is ResultState.Success -> _uiState.update { state ->
						val page = result.data
						state.copy(
							items = if (cursor == null) page.content else state.items + page.content,
							nextCursor = page.nextCursor.takeIf { !page.last },
							isLoading = false,
							isRefreshing = false,
							hasLoadedFirstPage = true
						)
					}

					is ResultState.Failure -> _uiState.update {
						it.copy(isLoading = false, isRefreshing = false, errorMessage = result.message)
					}

					else -> Unit
				}
			}
		}
	}

	private fun markAllRead() {
		unreadNotifications.clear()
		viewModelScope.launch {
			repository.markAllRead().collect { result ->
				// the server still counts them: show the badge again rather than hide unread entries
				if (result is ResultState.Failure) unreadNotifications.refresh()
			}
		}
	}

	companion object {
		const val PAGE_SIZE = 20
	}
}
