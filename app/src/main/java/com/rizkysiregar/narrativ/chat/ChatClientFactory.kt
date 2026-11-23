package com.rizkysiregar.narrativ.chat

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.rizkysiregar.marpokat.MarpokatClient
import com.rizkysiregar.marpokat.MarpokatOptions
import com.rizkysiregar.marpokat.TokenProvider
import com.rizkysiregar.marpokat.android.AppLifecycleConnector
import com.rizkysiregar.marpokat.android.RoomChatCache
import com.rizkysiregar.narrativ.data.remote.model.response.ChatTokenResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** A client with the way to stop it following the app. */
class ChatConnection(val client: MarpokatClient, val stop: () -> Unit)

/** Builds the client. Behind an interface because the Android parts (Room, process lifecycle) cannot run in a JVM test. */
interface ChatClientFactory {
	fun create(credentials: ChatTokenResponse, tokenProvider: TokenProvider): ChatConnection
}

@Singleton
class AndroidChatClientFactory @Inject constructor(
	@ApplicationContext private val context: Context
) : ChatClientFactory {
	override fun create(credentials: ChatTokenResponse, tokenProvider: TokenProvider): ChatConnection {
		val client = MarpokatClient(
			MarpokatOptions(
				baseUrl = credentials.baseUrl,
				apiKey = credentials.apiKey,
				tokenProvider = tokenProvider,
				// one database per user: nobody sees the conversations of the account that used this phone before
				cache = RoomChatCache(context, credentials.userId)
			)
		)
		// the process lifecycle can only be observed from the main thread, and this runs on a background one; the
		// posts keep their order, so a stop right after the start still finds what the start attached
		val main = Handler(Looper.getMainLooper())
		var detach: (() -> Unit)? = null
		main.post { detach = AppLifecycleConnector.attach(client) }
		return ChatConnection(client) { main.post { detach?.invoke() } }
	}
}
