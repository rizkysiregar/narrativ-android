package com.rizkysiregar.narrativ.testing.fakes

import com.rizkysiregar.narrativ.chat.ChatException
import com.rizkysiregar.narrativ.chat.ChatSession
import com.rizkysiregar.narrativ.chat.ChatSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A [ChatSession] the test drives: set [state], and read what the code under test asked for. */
class FakeChatSession(initial: ChatSessionState = ChatSessionState.Idle) : ChatSession {
	private val _state = MutableStateFlow(initial)
	override val state: StateFlow<ChatSessionState> = _state
	override var visibleChannelId: String? = null

	var starts = 0
	var signOuts = 0
	val pushTokens = mutableListOf<String>()
	var directMessage: (String) -> String = { "channel-of-$it" }
	var signOutFailure: Exception? = null

	fun set(state: ChatSessionState) { _state.value = state }

	override fun start() { starts++ }

	override suspend fun openDirectMessage(userId: String): String = directMessage(userId)

	override fun onPushToken(token: String) { pushTokens += token }

	override suspend fun signOut() {
		signOuts++
		signOutFailure?.let { throw it }
	}

	companion object {
		fun failingWith(error: ChatException) = FakeChatSession().also { it.directMessage = { throw error } }
	}
}
