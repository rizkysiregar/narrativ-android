package com.rizkysiregar.narrativ.ui.feature.home.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.marpokat.ChannelState
import com.rizkysiregar.marpokat.ConnectionStatus
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

/** One conversation in the list. The last message is kept as data so the screen words it in the app's language. */
data class ChatRow(
	val channelId: String,
	val title: String,
	val avatarUrl: String?,
	/** The Narrativ user id of the other person (a DM), for the avatar seed. */
	val peerId: String,
	val lastText: String?,
	/** `image`, `video` or `file` when the last message carries an attachment and no text. */
	val lastAttachmentKind: String?,
	val lastDeleted: Boolean,
	val lastAt: String?,
	val unread: Int,
	val online: Boolean
)

sealed interface ChatListUiState {
	data object Loading : ChatListUiState
	data object Unavailable : ChatListUiState
	data class Ready(val rows: List<ChatRow>, val offline: Boolean) : ChatListUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatListViewModel @Inject constructor(
	private val chat: ChatSession
) : ViewModel() {

	val uiState: StateFlow<ChatListUiState> = chat.state.flatMapLatest { session ->
		when (session) {
			is ChatSessionState.Ready -> session.client.state.map { state ->
				val rows = state.channels.map { rowOf(it, state.online) }
				// nothing cached and no connection yet: still loading, not empty
				if (rows.isEmpty() && state.status != ConnectionStatus.Connected) ChatListUiState.Loading
				else ChatListUiState.Ready(rows, offline = state.status != ConnectionStatus.Connected)
			}
			ChatSessionState.Unavailable -> flowOf(ChatListUiState.Unavailable)
			else -> flowOf(ChatListUiState.Loading)
		}
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatListUiState.Loading)

	/** Tries again after chat was unavailable. */
	fun retry() = chat.start()

	private fun rowOf(state: ChannelState, online: Map<String, Boolean>): ChatRow {
		val channel = state.channel
		val last = channel.lastMessage
		return ChatRow(
			channelId = channel.id,
			title = channel.title,
			avatarUrl = channel.dmPeer?.avatarUrl ?: channel.avatarUrl,
			peerId = channel.dmPeer?.externalId.orEmpty(),
			lastText = last?.text?.takeIf { it.isNotBlank() },
			lastAttachmentKind = last?.takeIf { it.text.isNullOrBlank() }?.attachments?.firstOrNull()?.kind,
			lastDeleted = last?.isDeleted == true,
			lastAt = channel.lastMessageAt ?: last?.createdAt,
			unread = channel.membership.unreadCount,
			online = channel.dmPeer?.externalId?.let { online[it] } == true
		)
	}
}
