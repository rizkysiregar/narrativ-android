package com.rizkysiregar.narrativ.ui.feature.home.notifications

import androidx.lifecycle.ViewModel
import com.rizkysiregar.narrativ.domain.notification.UnreadNotifications
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** The unread count for the bell on the feed. */
@HiltViewModel
class NotificationBadgeViewModel @Inject constructor(
	private val unreadNotifications: UnreadNotifications
) : ViewModel() {
	val unreadCount: StateFlow<Int> = unreadNotifications.count

	fun refresh() = unreadNotifications.refresh()
}
