package com.rizkysiregar.narrativ.ui.feature.home.chat

import com.rizkysiregar.marpokat.IncomingMessage

/**
 * What the in-app banner shows for a message that arrived while the app is open, in a conversation that is not on
 * screen. When the app is closed a push notification does this job instead (NarrativMessagingService).
 */
data class ChatBanner(
	val channelId: String,
	val messageId: String,
	val senderName: String,
	/** The group's name; null for a direct message. */
	val groupName: String?,
	/** The text, or null for a message that only carries a file (then [attachmentKind] says what it is). */
	val text: String?,
	val attachmentKind: String?,
	/** The sender's Narrativ user id (the seed of the generated avatar) and picture. */
	val senderId: String,
	val avatarUrl: String?,
)

/** The banner for a live message, or null when there should be none (the conversation is on screen). */
fun chatBannerOf(e: IncomingMessage, visibleChannelId: String?): ChatBanner? {
	if (e.isOpen || e.channel.id == visibleChannelId) return null
	val sender = e.message.sender
	val isGroup = e.channel.type == "group"
	return ChatBanner(
		channelId = e.channel.id,
		messageId = e.message.id,
		senderName = sender?.name?.takeIf { it.isNotBlank() } ?: e.channel.dmPeer?.name.orEmpty(),
		groupName = if (isGroup) e.channel.name?.takeIf { it.isNotBlank() } else null,
		text = e.message.text?.takeIf { it.isNotBlank() },
		attachmentKind = e.message.attachments.firstOrNull()?.kind,
		senderId = sender?.externalId.orEmpty(),
		avatarUrl = sender?.avatarUrl,
	)
}
