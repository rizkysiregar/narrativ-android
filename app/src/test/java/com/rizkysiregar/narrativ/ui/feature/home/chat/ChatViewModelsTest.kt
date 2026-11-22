package com.rizkysiregar.narrativ.ui.feature.home.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.marpokat.MarpokatClient
import com.rizkysiregar.narrativ.chat.ChatException
import com.rizkysiregar.narrativ.chat.ChatSessionState
import com.rizkysiregar.narrativ.chat.PickedFile
import com.rizkysiregar.narrativ.testing.MainDispatcherRule
import com.rizkysiregar.narrativ.testing.fakes.CHAT_CHANNEL
import com.rizkysiregar.narrativ.testing.fakes.FakeChatAttachmentReader
import com.rizkysiregar.narrativ.testing.fakes.FakeChatBackend
import com.rizkysiregar.narrativ.testing.fakes.FakeChatSession
import com.rizkysiregar.narrativ.testing.fakes.chatChannel
import com.rizkysiregar.narrativ.testing.fakes.chatMessage
import com.rizkysiregar.narrativ.testing.fakes.ME
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric only to create a Uri; the plain Application keeps Firebase out of it
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelsTest {
	@get:Rule
	val mainRule = MainDispatcherRule(StandardTestDispatcher())

	private val backend = FakeChatBackend()
	private val chat = FakeChatSession()
	private val reader = FakeChatAttachmentReader()
	private var client: MarpokatClient? = null

	// runTest ends by running all virtual time to the end; the client's ping loop never ends, so it is closed first
	private fun chatTest(body: suspend TestScope.() -> Unit): Unit = runTest(mainRule.dispatcher) {
		try { body() } finally { client?.close() }
	}

	// a connected client, wired into the session as Ready
	private suspend fun TestScope.ready() {
		val c = backend.client(mainRule.dispatcher).also { client = it }
		backend.connect(c)
		runCurrent()
		chat.set(ChatSessionState.Ready(c, "u-me"))
		runCurrent()
	}

	private fun TestScope.threadViewModel() = ChatThreadViewModel(
		SavedStateHandle(mapOf(ChatThreadViewModel.ARG_CHANNEL_ID to CHAT_CHANNEL)), chat, reader, backgroundScope
	).also { vm -> backgroundScope.launch { vm.uiState.collect { } } }

	// ---- list

	@Test fun theListIsLoadingUntilThereIsSomethingToShowThenListsTheConversations(): Unit = chatTest {
		backend.channels = listOf(chatChannel(unread = 2))
		val vm = ChatListViewModel(chat)
		backgroundScope.launch { vm.uiState.collect { } }
		runCurrent()
		assertThat(vm.uiState.value).isEqualTo(ChatListUiState.Loading)
		ready()
		val rows = (vm.uiState.value as ChatListUiState.Ready).rows
		assertThat(rows).hasSize(1)
		assertThat(rows[0].title).isEqualTo("Budi")
		assertThat(rows[0].lastText).isEqualTo("t2")
		assertThat(rows[0].unread).isEqualTo(2)
		assertThat(rows[0].online).isTrue()
		assertThat(rows[0].peerId).isEqualTo("u-budi")
	}

	@Test fun theListSaysSoWhenChatIsUnavailableAndRetryStartsTheSessionAgain(): Unit = chatTest {
		chat.set(ChatSessionState.Unavailable)
		val vm = ChatListViewModel(chat)
		backgroundScope.launch { vm.uiState.collect { } }
		runCurrent()
		assertThat(vm.uiState.value).isEqualTo(ChatListUiState.Unavailable)
		vm.retry()
		assertThat(chat.starts).isEqualTo(1)
	}

	// ---- badge

	@Test fun theBadgeStartsChatAndCountsUnreadMessages(): Unit = chatTest {
		backend.channels = listOf(chatChannel(unread = 3))
		val vm = ChatBadgeViewModel(chat)
		backgroundScope.launch { vm.unread.collect { } }
		assertThat(chat.starts).isEqualTo(1)
		assertThat(vm.unread.value).isEqualTo(0)
		ready()
		assertThat(vm.unread.value).isEqualTo(3)
		vm.refresh()
		assertThat(chat.starts).isEqualTo(2)
	}

	// ---- open chat

	@Test fun openingAChatGivesTheChannelAndFailuresTellWhetherThePersonCannotBeMessaged(): Unit = chatTest {
		val vm = OpenChatViewModel(chat)
		vm.open("u-budi"); runCurrent()
		assertThat(vm.state.value).isEqualTo(OpenChatState.Opened("channel-of-u-budi"))
		vm.consumed()
		assertThat(vm.state.value).isEqualTo(OpenChatState.Idle)

		chat.directMessage = { throw ChatException("gone", notFound = true) }
		vm.open("u-x"); runCurrent()
		assertThat(vm.state.value).isEqualTo(OpenChatState.Failed(notFound = true))
		chat.directMessage = { throw ChatException("down") }
		vm.open("u-x"); runCurrent()
		assertThat(vm.state.value).isEqualTo(OpenChatState.Failed(notFound = false))
	}

	// ---- thread

	@Test fun theThreadOpensItsChannelShowsTheMessagesAndMarksItselfAsOnScreen(): Unit = chatTest {
		ready()
		val vm = threadViewModel(); runCurrent()
		val state = vm.uiState.value
		assertThat(state.title).isEqualTo("Budi")
		assertThat(state.messages.map { it.text }).containsExactly("t1", "t2").inOrder()
		assertThat(state.myId).isEqualTo("u-me")
		assertThat(chat.visibleChannelId).isEqualTo(CHAT_CHANNEL)
		assertThat(backend.calls).contains("GET /v1/channels/$CHAT_CHANNEL/messages")
	}

	@Test fun aChannelThatCannotBeOpenedShowsAProblem(): Unit = chatTest {
		backend.failMessagesWith = 500
		ready()
		val vm = threadViewModel(); runCurrent()
		assertThat(vm.uiState.value.problem).isEqualTo(ChatThreadProblem.OpenFailed)
		vm.problemShown(); runCurrent()
		assertThat(vm.uiState.value.problem).isNull()
	}

	@Test fun sendingSendsTheTextWithTheReplyAndClearsTheMode(): Unit = chatTest {
		ready()
		val vm = threadViewModel(); runCurrent()
		vm.reply(vm.uiState.value.messages.last()); runCurrent()
		assertThat(vm.uiState.value.replyTo?.id).isEqualTo("m2")
		vm.send("  hello  "); runCurrent()
		val frame = backend.socket!!.frames.last { it.contains("message.send") }
		assertThat(frame).contains("\"text\":\"hello\"")
		assertThat(frame).contains("\"reply_to_id\":\"m2\"")
		assertThat(vm.uiState.value.replyTo).isNull()
		assertThat(vm.uiState.value.pending).hasSize(1)
	}

	@Test fun anEmptyMessageIsNotSent(): Unit = chatTest {
		ready()
		val vm = threadViewModel(); runCurrent()
		vm.send("   "); runCurrent()
		assertThat(backend.socket!!.frames.none { it.contains("message.send") }).isTrue()
	}

	@Test fun editingSendsTheNewTextOnlyWhenItChanged(): Unit = chatTest {
		backend.history = listOf(chatMessage(3, "mine", sender = ME))
		ready()
		val vm = threadViewModel(); runCurrent()
		val mine = vm.uiState.value.messages.first { it.id == "m3" }
		vm.startEdit(mine); runCurrent()
		assertThat(vm.uiState.value.editing).isEqualTo(mine)
		vm.send("mine"); runCurrent() // unchanged
		assertThat(backend.calls).doesNotContain("PATCH /v1/messages/m3")
		vm.startEdit(mine); runCurrent()
		vm.send("edited"); runCurrent()
		assertThat(backend.calls).contains("PATCH /v1/messages/m3")
		assertThat(vm.uiState.value.editing).isNull()
	}

	@Test fun reactingTogglesTheReactionAndDeletingRemovesTheMessage(): Unit = chatTest {
		ready()
		val vm = threadViewModel(); runCurrent()
		val message = vm.uiState.value.messages.last()
		vm.react(message, "👍"); runCurrent()
		assertThat(backend.calls).contains("PUT /v1/messages/m2/reactions/%F0%9F%91%8D")
		vm.delete(message); runCurrent()
		assertThat(backend.calls).contains("DELETE /v1/messages/m2")
	}

	@Test fun anAttachmentIsUploadedAndSentWithTheNextMessage(): Unit = chatTest {
		ready()
		val vm = threadViewModel(); runCurrent()
		vm.attach(Uri.parse("content://picked/1")); runCurrent()
		val upload = vm.uiState.value.uploads.single()
		assertThat(upload.attachment?.id).isEqualTo("att1")
		assertThat(upload.progress).isEqualTo(1f)
		assertThat(vm.uiState.value.canSend).isTrue()
		vm.send(""); runCurrent() // a message with only the picture
		val frame = backend.socket!!.frames.last { it.contains("message.send") }
		assertThat(frame).contains("\"attachment_ids\":[\"att1\"]")
		assertThat(vm.uiState.value.uploads).isEmpty()
	}

	@Test fun aFileThatCannotBeReadIsAProblemNotACrash(): Unit = chatTest {
		reader.file = null
		ready()
		val vm = threadViewModel(); runCurrent()
		vm.attach(Uri.parse("content://picked/1")); runCurrent()
		assertThat(vm.uiState.value.problem).isEqualTo(ChatThreadProblem.FileUnreadable)
		assertThat(vm.uiState.value.uploads).isEmpty()
	}

	@Test fun aFrozenChannelCannotBeWrittenIn(): Unit = chatTest {
		backend.channels = listOf(chatChannel(frozen = true))
		ready()
		val vm = threadViewModel(); runCurrent()
		assertThat(vm.uiState.value.frozen).isTrue()
		assertThat(vm.uiState.value.canSend).isFalse()
		vm.send("hi"); runCurrent()
		assertThat(backend.socket!!.frames.none { it.contains("message.send") }).isTrue()
	}

	@Test fun theThreadSaysSoWhenChatIsUnavailable(): Unit = chatTest {
		chat.set(ChatSessionState.Unavailable)
		val vm = threadViewModel(); runCurrent()
		assertThat(vm.uiState.value.unavailable).isTrue()
		assertThat(vm.uiState.value.loading).isFalse()
	}
}
