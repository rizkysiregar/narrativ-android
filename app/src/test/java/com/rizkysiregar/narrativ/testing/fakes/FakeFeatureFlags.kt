package com.rizkysiregar.narrativ.testing.fakes

import com.rizkysiregar.narrativ.config.FeatureFlags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** [FeatureFlags] the test drives: [chat] is the switch as last known, [answer] what the next refresh brings. */
class FakeFeatureFlags(chat: Boolean = true) : FeatureFlags {
	private val _chat = MutableStateFlow(chat)
	override val chatEnabled: StateFlow<Boolean> = _chat

	var answer: Boolean? = null
	var refreshes = 0

	fun setChat(enabled: Boolean) { _chat.value = enabled }

	override suspend fun refresh() {
		refreshes++
		answer?.let { _chat.value = it }
	}
}
