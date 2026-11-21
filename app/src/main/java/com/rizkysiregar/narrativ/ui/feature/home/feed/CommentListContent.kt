package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rizkysiregar.narrativ.data.remote.model.response.feed.CommentItem
import androidx.compose.ui.res.stringResource
import com.rizkysiregar.narrativ.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 08/03/26
 */

/**
 * One row of a post's comments. [postAuthorId] marks what the post's author wrote; [chapters] numbers the author's
 * own top-level comments as parts of the story (see [authorChapters]) and draws their threads in the accent color;
 * [highlightedId] is the comment a notification pointed at.
 */
@Composable
internal fun CommentListRowContent(
	row: CommentListRow,
	threadLine: ThreadLine,
	replyTargetId: String?,
	onReplyClick: (CommentItem) -> Unit,
	onToggleReplies: (String) -> Unit,
	onLoadMoreReplies: (String) -> Unit,
	onDeleteRequest: (CommentItem) -> Unit,
	onAuthorBlocked: () -> Unit,
	modifier: Modifier = Modifier,
	postAuthorId: String? = null,
	chapters: Map<String, Int> = emptyMap(),
	highlightedId: String? = null,
	// set when the viewer wrote the post: top-level comments can be pinned with a long press
	onPinToggle: ((CommentItem) -> Unit)? = null,
	onQuoteClick: (String) -> Unit = {}
) {
	val lineColor = if (row.threadId in chapters) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else threadLineColor()
	when (row) {
		is CommentListRow.Item -> {
			val comment = row.comment
			val commentRow: @Composable (Modifier) -> Unit = { rowModifier ->
				CommentRow(
					comment = comment,
					onReplyClick = onReplyClick,
					modifier = rowModifier,
					isReplyTarget = comment.id == replyTargetId,
					threadLine = threadLine,
					onAuthorBlocked = onAuthorBlocked,
					isPostAuthor = postAuthorId != null && comment.userId == postAuthorId,
					chapter = chapters[comment.id],
					lineColor = lineColor,
					isHighlighted = comment.id == highlightedId,
					onPinToggle = onPinToggle?.takeIf { !comment.isReply && !comment.isPending }?.let { toggle -> { toggle(comment) } },
					onQuoteClick = onQuoteClick
				)
			}
			if (comment.canDelete && !comment.isPending) {
				SwipeToDeleteComment(onDeleteRequest = { onDeleteRequest(comment) }, modifier = modifier) {
					commentRow(Modifier)
				}
			} else {
				commentRow(modifier)
			}
		}

		is CommentListRow.RepliesToggle -> ThreadControl(threadLine, lineColor, modifier) {
			RepliesActionText(
				text = if (row.expanded) stringResource(R.string.comment_hide_replies) else stringResource(R.string.comment_view_replies, row.replyCount),
				onClick = { onToggleReplies(row.rootId) }
			)
		}

		is CommentListRow.RepliesMore -> ThreadControl(threadLine, lineColor, modifier) {
			RepliesActionText(
				text = if (row.remaining != null) stringResource(R.string.comment_view_more_replies_n, row.remaining) else stringResource(R.string.comment_view_more_replies),
				onClick = { onLoadMoreReplies(row.rootId) }
			)
		}

		is CommentListRow.RepliesLoading -> ThreadControl(threadLine, lineColor, modifier) {
			CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
		}

		is CommentListRow.RepliesError -> ThreadControl(threadLine, lineColor, modifier) {
			RepliesActionText(
				text = stringResource(R.string.comment_replies_error),
				onClick = { onLoadMoreReplies(row.rootId) },
				isError = true
			)
		}
	}
}

/** A reply control sits on the thread line like a reply does, with the elbow at the middle of its text. */
@Composable
private fun ThreadControl(
	threadLine: ThreadLine,
	lineColor: Color,
	modifier: Modifier,
	content: @Composable () -> Unit
) {
	val endsThread = threadLine == ThreadLine.LastBranch
	Box(
		modifier = modifier
			.fillMaxWidth()
			.threadLine(threadLine, lineColor, elbowY = 14.dp)
			.padding(bottom = if (endsThread) 14.dp else 0.dp)
			.heightIn(min = 28.dp)
			.padding(start = ThreadIndent),
		contentAlignment = Alignment.CenterStart
	) {
		content()
	}
}

@Composable
private fun RepliesActionText(text: String, onClick: () -> Unit, isError: Boolean = false) {
	Text(
		text = text,
		style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
		color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
		modifier = Modifier
			.clip(RoundedCornerShape(8.dp))
			.clickable(onClick = onClick)
			.padding(vertical = 4.dp, horizontal = 2.dp)
	)
}


@Composable
internal fun DeleteCommentDialog(comment: CommentItem, onConfirm: () -> Unit, onDismiss: () -> Unit) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(if (comment.isReply) R.string.delete_reply_title else R.string.delete_comment_title)) },
		text = {
			Text(
				if (!comment.isReply && comment.replyCount > 0) {
					stringResource(R.string.delete_comment_with_replies, comment.replyCount)
				} else {
					stringResource(if (comment.isReply) R.string.delete_reply_body else R.string.delete_comment_body)
				}
			)
		},
		confirmButton = {
			TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
		},
		dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
	)
}

@Composable
fun EmptyCommentsView(onWriteClick: () -> Unit = {}, modifier: Modifier = Modifier) {
	Column(
		modifier = modifier.fillMaxWidth().padding(24.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = stringResource(R.string.comments_empty_title),
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.onSurface
		)
		Text(
			text = stringResource(R.string.comments_empty_body),
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
		)
		OutlinedButton(onClick = onWriteClick) { Text(stringResource(R.string.write_comment)) }
	}
}
