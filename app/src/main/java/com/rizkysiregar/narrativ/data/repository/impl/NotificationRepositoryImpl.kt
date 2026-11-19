package com.rizkysiregar.narrativ.data.repository.impl

import com.rizkysiregar.narrativ.data.remote.api.NotificationApi
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.notification.MarkNotificationReadRequest
import com.rizkysiregar.narrativ.data.remote.model.response.notification.NotificationPage
import com.rizkysiregar.narrativ.data.remote.model.response.notification.UnreadCountResponse
import com.rizkysiregar.narrativ.data.repository.NotificationRepository
import com.rizkysiregar.narrativ.util.flowFromApi
import com.rizkysiregar.narrativ.util.flowFromMessageApi
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
	private val retrofit: Retrofit
) : NotificationRepository {
	private val api: NotificationApi by lazy {
		retrofit.create(NotificationApi::class.java)
	}

	override fun getNotifications(cursor: String?, size: Int): Flow<ResultState<NotificationPage>> =
		flowFromApi { api.getNotifications(cursor, size) }

	override fun getUnreadCount(): Flow<ResultState<UnreadCountResponse>> =
		flowFromApi { api.getUnreadCount() }

	override fun markAllRead(): Flow<ResultState<String>> =
		flowFromMessageApi { api.markAllRead() }

	override fun markRead(groupKey: String): Flow<ResultState<String>> =
		flowFromMessageApi { api.markRead(MarkNotificationReadRequest(groupKey)) }
}
