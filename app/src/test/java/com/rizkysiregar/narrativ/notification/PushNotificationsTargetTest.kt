package com.rizkysiregar.narrativ.notification

import com.rizkysiregar.narrativ.notification.PushNotifications.PushTarget
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushNotificationsTargetTest {

	private fun target(vararg pairs: Pair<String, String?>) = PushNotifications.targetFor(mapOf(*pairs))

	@Test
	fun newPostOpensThePost() {
		val result = target("type" to "NEW_POST", "postId" to "p1", "authorId" to "u1", "authorUsername" to "ani")

		assertEquals(PushTarget.Post("p1", openComments = false), result)
	}

	@Test
	fun likeOpensThePostWithoutComments() {
		val result = target("type" to "POST_LIKED", "postId" to "p1", "actorId" to "u2", "actorUsername" to "budi")

		assertEquals(PushTarget.Post("p1", openComments = false), result)
	}

	@Test
	fun commentAndReplyOpenThePostAtTheirComment() {
		for (type in listOf("POST_COMMENTED", "COMMENT_REPLIED")) {
			val result = target("type" to type, "postId" to "p1", "commentId" to "c1", "actorId" to "u2", "actorUsername" to "budi")

			assertEquals(type, PushTarget.Post("p1", openComments = true, commentId = "c1"), result)
		}
	}

	@Test
	fun aPushWithoutAPostIdFallsBackToTheProfile() {
		assertEquals(
			PushTarget.Profile("u1", "ani"),
			target("type" to "NEW_POST", "authorId" to "u1", "authorUsername" to "ani")
		)
		assertEquals(
			PushTarget.Profile("u2", "budi"),
			target("type" to "POST_LIKED", "postId" to " ", "actorId" to "u2", "actorUsername" to "budi")
		)
	}

	@Test
	fun unknownOrIncompletePushesGoNowhere() {
		assertNull(target("type" to "SOMETHING_ELSE", "postId" to "p1"))
		assertNull(target("postId" to "p1"))
		assertNull(target("type" to "NEW_POST", "authorId" to "u1"))
		assertNull(PushNotifications.targetFor(emptyMap()))
	}

	@Test
	fun aFollowOpensTheFollowersProfile() {
		val result = target("type" to "FOLLOWED", "actorId" to "u2", "actorUsername" to "budi")

		assertEquals(PushTarget.Profile("u2", "budi"), result)
	}

	@Test
	fun aPinOpensThePostAtThePinnedComment() {
		val result = target("type" to "COMMENT_PINNED", "postId" to "p1", "commentId" to "c1", "actorId" to "u2", "actorUsername" to "budi")

		assertEquals(PushTarget.Post("p1", openComments = true, commentId = "c1"), result)
	}

	@Test
	fun aChatMessageOpensItsConversation() {
		val result = target("marpokat" to "1", "type" to "message.new", "channel_id" to "ch-9", "sender_name" to "Budi")

		assertEquals(PushTarget.Chat("ch-9"), result)
	}

	@Test
	fun aChatPushWithoutAChannelOpensNothingAndIsNotMistakenForAPostPush() {
		assertEquals(null, target("marpokat" to "1", "type" to "message.new"))
		assertEquals(null, target("marpokat" to "1", "type" to "POST_LIKED", "postId" to "p1", "channel_id" to " "))
	}

	@Test
	fun aChatPushIsHiddenOnlyForTheConversationOnScreenWhileTheAppIsOpen() {
		assertThat(PushNotifications.shouldShowChat("ch-1", visibleChannelId = "ch-1", appInForeground = true)).isFalse()
		assertThat(PushNotifications.shouldShowChat("ch-1", visibleChannelId = "ch-2", appInForeground = true)).isTrue()
		assertThat(PushNotifications.shouldShowChat("ch-1", visibleChannelId = null, appInForeground = true)).isTrue()
		// left in the background from that very thread: shown
		assertThat(PushNotifications.shouldShowChat("ch-1", visibleChannelId = "ch-1", appInForeground = false)).isTrue()
	}
}
