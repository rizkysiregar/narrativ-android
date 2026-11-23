package com.rizkysiregar.narrativ.chat

import com.rizkysiregar.marpokat.MarpokatClient
import kotlinx.coroutines.flow.StateFlow

/** Where the chat connection of the signed-in user stands. */
sealed interface ChatSessionState {
	/** Not started: nobody is signed in, or the user signed out. */
	data object Idle : ChatSessionState

	data object Starting : ChatSessionState

	/** The chat service is off or cannot be reached. The rest of the app works; [ChatSession.start] tries again. */
	data object Unavailable : ChatSessionState

	data class Ready(val client: MarpokatClient, val userId: String) : ChatSessionState
}

/** A chat is not possible right now: the service is off, unreachable, or the user may not message that person. */
class ChatException(message: String, val notFound: Boolean = false, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * The chat connection of the signed-in user: it asks Narrativ's backend for a chat token, builds the client, keeps it
 * connected while the app is in the foreground and registers the push token. One per process.
 */
interface ChatSession {
	val state: StateFlow<ChatSessionState>

	/** Starts when nobody started yet (or the last try failed). Safe to call whenever the home screen shows. */
	fun start()

	/** Opens (creating it when needed) the direct message with a Narrativ user and returns its channel id. */
	suspend fun openDirectMessage(userId: String): String

	/** The channel whose thread is on screen: a push about it is not shown, its messages are right there. */
	var visibleChannelId: String?

	/** FCM gave the app a new push token. */
	fun onPushToken(token: String)

	/** Forgets the chat of this user: the push token is removed, the connection closed and the local cache emptied. */
	suspend fun signOut()
}
