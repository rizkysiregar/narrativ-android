package com.rizkysiregar.feedzmore.ui.feature.home.feed

import com.rizkysiregar.feedzmore.data.remote.model.response.feed.CommentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentListRowsTest {

	private fun root(id: String, replyCount: Int = 0) =
		CommentItem(id = id, username = "u$id", commentContent = "c$id", replyCount = replyCount)

	private fun reply(id: String, rootId: String, replyTo: String = rootId) =
		CommentItem(id = id, username = "u$id", commentContent = "c$id", parentId = rootId, replyToCommentId = replyTo)

	private fun keys(rows: List<CommentListRow>) = rows.map { it.key }

	@Test
	fun collapsedCommentWithRepliesOffersToShowThem() {
		val rows = buildCommentRows(listOf(root("a", replyCount = 3), root("b")), emptyMap())

		assertEquals(listOf("a", "toggle-a", "b"), keys(rows))
		assertEquals(CommentListRow.RepliesToggle("a", 3, expanded = false), rows[1])
	}

	@Test
	fun expandedCommentShowsItsRepliesThenMoreAndHide() {
		val state = RepliesState(items = listOf(reply("r1", "a"), reply("r2", "a", replyTo = "r1")), nextPage = 1, hasMore = true)
		val rows = buildCommentRows(listOf(root("a", replyCount = 5)), mapOf("a" to state))

		assertEquals(listOf("a", "r1", "r2", "more-a", "toggle-a"), keys(rows))
		assertEquals(CommentListRow.RepliesMore("a", remaining = 3), rows[3])
		assertEquals(CommentListRow.RepliesToggle("a", 5, expanded = true), rows[4])
	}

	@Test
	fun lastPageOfRepliesHasNoMoreButton() {
		val state = RepliesState(items = listOf(reply("r1", "a")), nextPage = 1, hasMore = false)
		val rows = buildCommentRows(listOf(root("a", replyCount = 1)), mapOf("a" to state))

		assertEquals(listOf("a", "r1", "toggle-a"), keys(rows))
	}

	@Test
	fun loadingAndErrorReplaceTheMoreButton() {
		val loading = RepliesState(isLoading = true)
		val failed = RepliesState(errorMessage = "offline")

		assertEquals(listOf("a", "loading-a", "toggle-a"), keys(buildCommentRows(listOf(root("a", 2)), mapOf("a" to loading))))
		assertEquals(listOf("a", "error-a", "toggle-a"), keys(buildCommentRows(listOf(root("a", 2)), mapOf("a" to failed))))
	}

	@Test
	fun lastReplyIndexFindsTheNewestReplyOfThatThread() {
		val state = RepliesState(items = listOf(reply("r1", "a"), reply("r2", "a")), hasMore = false)
		val rows = buildCommentRows(listOf(root("a", 2), root("b")), mapOf("a" to state))

		assertEquals(2, rows.lastReplyIndex("a"))
		assertEquals(-1, rows.lastReplyIndex("b"))
		assertTrue(rows.lastIndex > 2)
	}

	@Test
	fun threadLineTiesRepliesAndControlsToTheirTopLevelComment() {
		val state = RepliesState(items = listOf(reply("r1", "a"), reply("r2", "a")), hasMore = true)
		val rows = buildCommentRows(listOf(root("a", 5), root("b", 1), root("c")), mapOf("a" to state))
		// a, r1, r2, more-a, toggle-a, b, toggle-b, c
		assertEquals(listOf("a", "r1", "r2", "more-a", "toggle-a", "b", "toggle-b", "c"), keys(rows))

		val lines = rows.indices.map { rows.threadLineAt(it) }
		assertEquals(
			listOf(
				ThreadLine.RootStart, ThreadLine.Branch, ThreadLine.Branch, ThreadLine.Branch, ThreadLine.LastBranch,
				ThreadLine.RootStart, ThreadLine.LastBranch,
				ThreadLine.None
			),
			lines
		)
	}
}
