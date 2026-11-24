package com.rizkysiregar.narrativ.ui.feature.home.chat

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.marpokat.Attachment
import com.rizkysiregar.marpokat.Channel
import com.rizkysiregar.marpokat.IncomingMessage
import com.rizkysiregar.marpokat.Message
import com.rizkysiregar.marpokat.User
import org.junit.Test

class ChatBannerTest {
	private val budi = User(id = "m-budi", externalId = "u-budi", name = "Budi", avatarUrl = "https://pics.test/budi.png")
	private val dm = Channel(id = "ch-dm", type = "dm", dmPeer = budi)
	private val group = Channel(id = "ch-g", type = "group", name = "Hiking")

	private fun incoming(channel: Channel, text: String? = "Halo", open: Boolean = false, attachments: List<Attachment> = emptyList()) =
		IncomingMessage(Message(id = "m1", channelId = channel.id, seq = 1, revSeq = 1, sender = budi, text = text, attachments = attachments), channel, open)

	@Test
	fun aDirectMessageShowsTheSenderAndTheText() {
		val b = chatBannerOf(incoming(dm), visibleChannelId = null)!!
		assertThat(b.channelId).isEqualTo("ch-dm")
		assertThat(b.senderName).isEqualTo("Budi")
		assertThat(b.groupName).isNull()
		assertThat(b.text).isEqualTo("Halo")
		assertThat(b.senderId).isEqualTo("u-budi")
		assertThat(b.avatarUrl).isEqualTo("https://pics.test/budi.png")
	}

	@Test
	fun aGroupMessageNamesTheGroup() {
		assertThat(chatBannerOf(incoming(group), null)!!.groupName).isEqualTo("Hiking")
	}

	@Test
	fun aFileWithoutTextSaysWhatItIs() {
		val b = chatBannerOf(incoming(dm, text = " ", attachments = listOf(Attachment(id = "a1", kind = "image"))), null)!!
		assertThat(b.text).isNull()
		assertThat(b.attachmentKind).isEqualTo("image")
	}

	@Test
	fun noBannerForTheConversationOnScreen() {
		assertThat(chatBannerOf(incoming(dm, open = true), null)).isNull()
		assertThat(chatBannerOf(incoming(dm), visibleChannelId = "ch-dm")).isNull()
		assertThat(chatBannerOf(incoming(dm), visibleChannelId = "ch-other")).isNotNull()
	}
}
