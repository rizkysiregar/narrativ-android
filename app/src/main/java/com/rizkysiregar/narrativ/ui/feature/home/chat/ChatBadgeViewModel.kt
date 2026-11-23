package com.rizkysiregar.narrativ.ui.feature.home.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.chat.ChatSession
import com.rizkysiregar.narrativ.chat.ChatSessionState
import com.rizkysiregar.narrativ.config.FeatureFlags
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Starts the chat when the home screen shows, and counts unread messages for the badge on the feed. While an admin
 * has chat switched off (feature.chat.enabled) the chat is stopped instead: no connection, no chat pushes, no badge.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatBadgeViewModel @Inject constructor(
	private val chat: ChatSession,
	private val featureFlags: FeatureFlags
) : ViewModel() {

	/** Whether the messages icon, the Message button and the chat screens show. */
	val chatEnabled: StateFlow<Boolean> = featureFlags.chatEnabled

	init {
		viewModelScope.launch {
			// the last known switch acts at once; the backend's answer, when different, follows
			featureFlags.chatEnabled.collect { enabled ->
				// switched off: closes the connection and removes the push token, so no chat push arrives either
				if (enabled) chat.start() else chat.signOut()
			}
		}
		viewModelScope.launch { featureFlags.refresh() }
	}

	val unread: StateFlow<Int> = combine(featureFlags.chatEnabled, chat.state) { enabled, session -> enabled to session }
		.flatMapLatest { (enabled, session) ->
			if (enabled && session is ChatSessionState.Ready) session.client.state.map { it.unreadTotal } else flowOf(0)
		}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

	/** Back in the foreground: ask for the switches again, and start again if chat was off or never started. */
	fun refresh() {
		if (featureFlags.chatEnabled.value) chat.start()
		viewModelScope.launch { featureFlags.refresh() }
	}
}
