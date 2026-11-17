package com.rizkysiregar.narrativ.notification

import com.rizkysiregar.narrativ.notification.PushNotifications.PushTarget
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
	fun commentAndReplyOpenThePostWithItsComments() {
		for (type in listOf("POST_COMMENTED", "COMMENT_REPLIED")) {
			val result = target("type" to type, "postId" to "p1", "commentId" to "c1", "actorId" to "u2", "actorUsername" to "budi")

			assertEquals(type, PushTarget.Post("p1", openComments = true), result)
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
}
