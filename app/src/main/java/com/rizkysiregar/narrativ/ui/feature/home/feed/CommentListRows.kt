package com.rizkysiregar.narrativ.ui.feature.home.feed

import com.rizkysiregar.narrativ.data.remote.model.response.feed.CommentItem

/** Flat rows of the comment list: a top-level comment is followed by the controls and rows of its replies. */
internal sealed interface CommentListRow {
	val key: String

	/** The top-level comment this row belongs to. */
	val threadId: String

	data class Item(val comment: CommentItem) : CommentListRow {
		override val key: String get() = comment.id
		override val threadId: String get() = comment.rootId
	}

	/** "Lihat 3 balasan" while collapsed, "Sembunyikan balasan" while expanded. */
	data class RepliesToggle(val rootId: String, val replyCount: Int, val expanded: Boolean) : CommentListRow {
		override val key: String get() = "toggle-$rootId"
		override val threadId: String get() = rootId
	}

	/** "Lihat balasan lainnya", [remaining] is unknown (null) when the count is not reliable. */
	data class RepliesMore(val rootId: String, val remaining: Int?) : CommentListRow {
		override val key: String get() = "more-$rootId"
		override val threadId: String get() = rootId
	}

	data class RepliesLoading(val rootId: String) : CommentListRow {
		override val key: String get() = "loading-$rootId"
		override val threadId: String get() = rootId
	}

	data class RepliesError(val rootId: String, val message: String) : CommentListRow {
		override val key: String get() = "error-$rootId"
		override val threadId: String get() = rootId
	}
}

internal fun buildCommentRows(
	comments: List<CommentItem>,
	replies: Map<String, RepliesState>
): List<CommentListRow> = buildList {
	for (comment in comments) {
		add(CommentListRow.Item(comment))
		val state = replies[comment.id]
		if (state == null) {
			if (comment.replyCount > 0) add(CommentListRow.RepliesToggle(comment.id, comment.replyCount, expanded = false))
			continue
		}
		state.items.forEach { add(CommentListRow.Item(it)) }
		when {
			state.isLoading -> add(CommentListRow.RepliesLoading(comment.id))
			state.errorMessage != null -> add(CommentListRow.RepliesError(comment.id, state.errorMessage))
			state.hasMore -> add(
				CommentListRow.RepliesMore(
					rootId = comment.id,
					remaining = (comment.replyCount - state.items.size).takeIf { it > 0 }
				)
			)
		}
		add(CommentListRow.RepliesToggle(comment.id, comment.replyCount, expanded = true))
	}
}

/** Index of the newest loaded reply of [rootId], or -1. */
internal fun List<CommentListRow>.lastReplyIndex(rootId: String): Int =
	indexOfLast { it is CommentListRow.Item && it.comment.parentId == rootId }

/** Which part of the thread line the row at [index] draws. */
internal fun List<CommentListRow>.threadLineAt(index: Int): ThreadLine {
	val row = this[index]
	val continues = getOrNull(index + 1)?.threadId == row.threadId
	val isTopLevel = row is CommentListRow.Item && !row.comment.isReply
	return when {
		isTopLevel -> if (continues) ThreadLine.RootStart else ThreadLine.None
		continues -> ThreadLine.Branch
		else -> ThreadLine.LastBranch
	}
}
