package com.rizkysiregar.narrativ.data.remote.api

import com.rizkysiregar.narrativ.data.remote.model.request.notification.MarkNotificationReadRequest
import com.rizkysiregar.narrativ.data.remote.model.response.BaseResponse
import com.rizkysiregar.narrativ.data.remote.model.response.notification.NotificationPage
import com.rizkysiregar.narrativ.data.remote.model.response.notification.UnreadCountResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** The signed-in user's inbox: likes, comments, replies, follows and pins. */
interface NotificationApi {

	@GET("users/me/notifications")
	suspend fun getNotifications(
		@Query("cursor") cursor: String?,
		@Query("size") size: Int
	): BaseResponse<NotificationPage>

	@GET("users/me/notifications/unread-count")
	suspend fun getUnreadCount(): BaseResponse<UnreadCountResponse>

	@POST("users/me/notifications/read-all")
	suspend fun markAllRead(): BaseResponse<String>

	@POST("users/me/notifications/read")
	suspend fun markRead(@Body request: MarkNotificationReadRequest): BaseResponse<String>
}
