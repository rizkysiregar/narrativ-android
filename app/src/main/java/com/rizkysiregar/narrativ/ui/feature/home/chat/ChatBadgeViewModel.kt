package com.rizkysiregar.narrativ.ui.feature.home.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.chat.ChatSession
import com.rizkysiregar.narrativ.chat.ChatSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Starts the chat when the home screen shows, and counts unread messages for the badge on the feed. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatBadgeViewModel @Inject constructor(
	private val chat: ChatSession
) : ViewModel() {

	init {
		chat.start()
	}

	val unread: StateFlow<Int> = chat.state.flatMapLatest { session ->
		if (session is ChatSessionState.Ready) session.client.state.map { it.unreadTotal } else flowOf(0)
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

	/** Back in the foreground: start again if chat was off or never started. */
	fun refresh() = chat.start()
}
