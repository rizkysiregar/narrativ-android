package com.rizkysiregar.narrativ.domain.notification

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.repository.NotificationRepository
import com.rizkysiregar.narrativ.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * How many inbox entries are unread, for the badge on the bell. Asked again when a push arrives while the app is open,
 * when the home screen comes back to the foreground and after the inbox was read. A failed request keeps the last
 * count: a stale badge beats a badge that blinks away.
 */
@Singleton
class UnreadNotifications @Inject constructor(
	private val repository: NotificationRepository,
	@ApplicationScope private val scope: CoroutineScope
) {
	private val _count = MutableStateFlow(0)
	val count: StateFlow<Int> = _count.asStateFlow()

	private var refreshJob: Job? = null

	fun refresh() {
		if (refreshJob?.isActive == true) return
		refreshJob = scope.launch {
			repository.getUnreadCount().collect { result ->
				if (result is ResultState.Success) _count.value = result.data.unread
			}
		}
	}

	/** The inbox was opened and marked read. */
	fun clear() {
		_count.value = 0
	}
}
