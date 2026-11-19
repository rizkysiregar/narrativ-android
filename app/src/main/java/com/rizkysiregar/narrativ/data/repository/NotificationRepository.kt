package com.rizkysiregar.narrativ.data.repository

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.notification.NotificationPage
import com.rizkysiregar.narrativ.data.remote.model.response.notification.UnreadCountResponse
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
	fun getNotifications(cursor: String?, size: Int): Flow<ResultState<NotificationPage>>
	fun getUnreadCount(): Flow<ResultState<UnreadCountResponse>>
	fun markAllRead(): Flow<ResultState<String>>
	fun markRead(groupKey: String): Flow<ResultState<String>>
}
