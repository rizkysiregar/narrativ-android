package com.rizkysiregar.narrativ.data.remote.api

import com.rizkysiregar.narrativ.data.remote.model.response.BaseResponse
import com.rizkysiregar.narrativ.data.remote.model.response.ChatTokenResponse
import com.rizkysiregar.narrativ.data.remote.model.response.DirectMessageResponse
import retrofit2.http.POST
import retrofit2.http.Path

/** What Narrativ's backend gives the app to talk to the chat service (Marpokat). Messages themselves go to Marpokat. */
interface ChatApi {

	/** A Marpokat user token with the api key and address of this environment. 503 while chat is off or unreachable. */
	@POST("chat/token")
	suspend fun token(): BaseResponse<ChatTokenResponse>

	/** The direct message with another user, created when it does not exist yet. */
	@POST("chat/dm/{userId}")
	suspend fun openDirectMessage(@Path("userId") userId: String): BaseResponse<DirectMessageResponse>
}
