package com.rizkysiregar.narrativ.ui.feature.home.feed

import com.rizkysiregar.narrativ.data.remote.model.response.feed.CommentItem
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthorChaptersTest {

	private fun comment(id: String, userId: String, parentId: String? = null, status: String? = null) =
		CommentItem(id = id, username = userId, userId = userId, parentId = parentId, moderationStatus = status)

	@Test
	fun theAuthorsTopLevelCommentsArePartsTwoOnwardInListOrder() {
		val comments = listOf(
			comment("c1", "author"),
			comment("c2", "reader"),
			comment("c3", "author")
		)

		assertEquals(mapOf("c1" to 2, "c3" to 3), authorChapters(comments, "author"))
	}

	@Test
	fun repliesAndTakenDownCommentsAreNotParts() {
		val comments = listOf(
			comment("c1", "reader"),
			comment("r1", "author", parentId = "c1"),
			comment("c2", "author", status = CommentItem.MODERATION_TAKEN_DOWN),
			comment("c3", "author")
		)

		assertEquals(mapOf("c3" to 2), authorChapters(comments, "author"))
	}

	@Test
	fun withoutAKnownAuthorNothingIsAPart() {
		assertEquals(emptyMap<String, Int>(), authorChapters(listOf(comment("c1", "author")), null))
	}
}
