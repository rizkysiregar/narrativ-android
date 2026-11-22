package com.rizkysiregar.narrativ.data.remote.model.response

/** What the app needs to connect to the chat service; [expiresAt] is an ISO instant. */
data class ChatTokenResponse(
	val token: String,
	val expiresAt: String? = null,
	val apiKey: String,
	val baseUrl: String,
	val userId: String
)

data class DirectMessageResponse(val channelId: String)
