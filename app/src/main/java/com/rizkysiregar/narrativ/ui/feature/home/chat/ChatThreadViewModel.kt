package com.rizkysiregar.narrativ.ui.feature.home.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.marpokat.Attachment
import com.rizkysiregar.marpokat.ChatState
import com.rizkysiregar.marpokat.MarpokatClient
import com.rizkysiregar.marpokat.Message
import com.rizkysiregar.marpokat.PendingMessage
import com.rizkysiregar.narrativ.chat.ChatAttachmentReader
import com.rizkysiregar.narrativ.chat.ChatSession
import com.rizkysiregar.narrativ.chat.ChatSessionState
import com.rizkysiregar.narrativ.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A file being sent or ready to send with the next message. */
data class UploadItem(val key: Int, val name: String, val progress: Float, val attachment: Attachment? = null)

/** Why something did not work, for the screen to word. */
enum class ChatThreadProblem { OpenFailed, UploadFailed, FileUnreadable, ActionFailed }

data class ChatThreadUiState(
	val loading: Boolean = true,
	val unavailable: Boolean = false,
	val title: String = "",
	val frozen: Boolean = false,
	val group: Boolean = false,
	val myId: String = "",
	val messages: List<Message> = emptyList(),
	val pending: List<PendingMessage> = emptyList(),
	val hasOlder: Boolean = false,
	val typing: List<String> = emptyList(),
	val replyTo: Message? = null,
	val editing: Message? = null,
	val uploads: List<UploadItem> = emptyList(),
	val problem: ChatThreadProblem? = null
) {
	val canSend: Boolean get() = uploads.none { it.attachment == null } && !frozen
}

private data class Local(
	val replyTo: Message? = null,
	val editing: Message? = null,
	val uploads: List<UploadItem> = emptyList(),
	val problem: ChatThreadProblem? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatThreadViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val chat: ChatSession,
	private val reader: ChatAttachmentReader,
	@ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {

	private val channelId: String = savedStateHandle.get<String>(ARG_CHANNEL_ID).orEmpty()
	private val local = MutableStateFlow(Local())
	private var nextUploadKey = 0
	private var client: MarpokatClient? = null

	val uiState: StateFlow<ChatThreadUiState> = chat.state.flatMapLatest { session ->
		when (session) {
			is ChatSessionState.Ready -> combine(session.client.state, local) { state, local -> build(state, local) }
			ChatSessionState.Unavailable -> flowOf(ChatThreadUiState(loading = false, unavailable = true))
			else -> flowOf(ChatThreadUiState())
		}
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatThreadUiState())

	init {
		chat.visibleChannelId = channelId
		viewModelScope.launch {
			val ready = chat.state.filterIsInstance<ChatSessionState.Ready>().first()
			client = ready.client
			try {
				ready.client.openChannel(channelId)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				local.update { it.copy(problem = ChatThreadProblem.OpenFailed) }
			}
		}
	}

	private fun build(state: ChatState, local: Local): ChatThreadUiState {
		val channel = state.channel(channelId)
		val peer = channel?.channel?.dmPeer
		return ChatThreadUiState(
			loading = channel == null || !channel.loaded && state.messages[channelId].isNullOrEmpty(),
			title = channel?.channel?.title.orEmpty(),
			frozen = channel?.channel?.frozen == true,
			group = channel?.channel?.type == "group",
			myId = state.me?.externalId.orEmpty(),
			messages = state.messages[channelId].orEmpty(),
			pending = state.pending[channelId].orEmpty(),
			hasOlder = channel?.hasOlder == true,
			typing = state.typing[channelId].orEmpty().map { id -> if (id == peer?.externalId) peer.name.ifBlank { id } else id },
			replyTo = local.replyTo,
			editing = local.editing,
			uploads = local.uploads,
			problem = local.problem
		)
	}

	fun send(text: String) {
		val state = uiState.value
		val editing = state.editing
		val trimmed = text.trim()
		if (editing != null) {
			if (trimmed.isNotEmpty() && trimmed != editing.text) act { it.edit(editing.id, trimmed) }
			local.update { it.copy(editing = null) }
			return
		}
		val attachments = state.uploads.mapNotNull { it.attachment?.id }
		if ((trimmed.isEmpty() && attachments.isEmpty()) || !state.canSend) return
		val replyId = state.replyTo?.id
		local.update { it.copy(replyTo = null, uploads = emptyList()) }
		viewModelScope.launch {
			client?.send(channelId, trimmed, attachments, replyId)
			client?.typingStop(channelId)
		}
	}

	fun onTyping() {
		viewModelScope.launch { client?.typingStart(channelId) }
	}

	fun reply(message: Message) = local.update { it.copy(replyTo = message, editing = null) }
	fun startEdit(message: Message) = local.update { it.copy(editing = message, replyTo = null) }
	fun cancelComposerMode() = local.update { it.copy(replyTo = null, editing = null) }

	fun react(message: Message, emoji: String) {
		val on = emoji !in message.reactions.mine
		act { it.react(message.id, emoji, on) }
	}

	fun delete(message: Message) = act { it.remove(message.id) }
	fun retry(clientMsgId: String) = act { it.retry(clientMsgId) }
	fun discard(clientMsgId: String) = act { it.discard(clientMsgId) }

	fun loadOlder() {
		viewModelScope.launch { runCatching { client?.loadOlder(channelId) } }
	}

	fun attach(uri: Uri) {
		val key = nextUploadKey++
		viewModelScope.launch {
			val file = reader.read(uri)
			if (file == null) {
				local.update { it.copy(problem = ChatThreadProblem.FileUnreadable) }
				return@launch
			}
			local.update { it.copy(uploads = it.uploads + UploadItem(key, file.name ?: "file", 0f)) }
			try {
				val attachment = requireNotNull(client).upload(file.bytes, file.mime, file.name) { progress ->
					val fraction = if (progress.total > 0) progress.loaded.toFloat() / progress.total else 0f
					local.update { it.copy(uploads = it.uploads.map { u -> if (u.key == key) u.copy(progress = fraction) else u }) }
				}
				local.update { it.copy(uploads = it.uploads.map { u -> if (u.key == key) u.copy(progress = 1f, attachment = attachment) else u }) }
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				local.update { it.copy(uploads = it.uploads.filterNot { u -> u.key == key }, problem = ChatThreadProblem.UploadFailed) }
			}
		}
	}

	fun removeUpload(key: Int) = local.update { it.copy(uploads = it.uploads.filterNot { u -> u.key == key }) }

	fun problemShown() = local.update { it.copy(problem = null) }

	private fun act(block: suspend (MarpokatClient) -> Unit) {
		val current = client ?: return
		viewModelScope.launch {
			try {
				block(current)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				local.update { it.copy(problem = ChatThreadProblem.ActionFailed) }
			}
		}
	}

	override fun onCleared() {
		chat.visibleChannelId = null
		val current = client ?: return
		// the view model scope is gone: closing the channel must still happen, or its unread counting stays off
		appScope.launch { current.closeChannel(channelId) }
	}

	companion object {
		const val ARG_CHANNEL_ID = "channelId"
	}
}
