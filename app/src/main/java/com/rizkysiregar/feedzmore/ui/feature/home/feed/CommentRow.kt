package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.feedzmore.ui.components.ProfileAvatar
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme
import com.rizkysiregar.feedzmore.util.formatTimeAgo

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 08/03/26
 * Description: Software Engineer
 */

/**
 * A comment or a reply. Replies are drawn smaller and indented, tied to their top-level comment by the
 * [threadLine], and start with the @tag of the user they answer, so a thread stays readable even though
 * it is only one level deep. [isReplyTarget] tints the comment the input is currently answering.
 */
@Composable
fun CommentRow(
	comment: CommentItem,
	onReplyClick: (CommentItem) -> Unit,
	modifier: Modifier = Modifier,
	isReplyTarget: Boolean = false
) {
	CommentRow(comment, onReplyClick, modifier, isReplyTarget, ThreadLine.None)
}

@Composable
internal fun CommentRow(
	comment: CommentItem,
	onReplyClick: (CommentItem) -> Unit,
	modifier: Modifier,
	isReplyTarget: Boolean,
	threadLine: ThreadLine
) {
	val isReply = comment.isReply
	val lineColor = threadLineColor()
	val highlight by animateColorAsState(
		targetValue = if (isReplyTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
		label = "replyTargetHighlight"
	)
	val endsThread = threadLine == ThreadLine.None || threadLine == ThreadLine.LastBranch

	Row(
		modifier = modifier
			.fillMaxWidth()
			.alpha(if (comment.isPending) 0.55f else 1f)
			.threadLine(threadLine, lineColor, elbowY = if (isReply) 20.dp else 0.dp)
			.padding(top = 6.dp, bottom = if (endsThread) 8.dp else 6.dp)
			.padding(start = if (isReply) ThreadIndent else 0.dp)
			.drawBehind {
				// outset a little so the text does not touch the edge of the tint
				val outset = 6.dp.toPx()
				drawRoundRect(
					color = highlight,
					topLeft = Offset(-outset, -4.dp.toPx()),
					size = Size(size.width + 2 * outset, size.height + 8.dp.toPx()),
					cornerRadius = CornerRadius(12.dp.toPx())
				)
			},
		horizontalArrangement = Arrangement.spacedBy(12.dp)
	) {
		ProfileAvatar(
			imageUrl = comment.profileImageUrl,
			userId = "", // no photo: initials, like the rest of the feed
			userName = comment.username,
			size = if (isReply) 28.dp else 36.dp
		)

		Column(modifier = Modifier.weight(1f)) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = comment.username,
					style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
				)
				val timeAgo = if (comment.isPending) "Mengirim..." else formatTimeAgo(comment.createdAt)
				if (timeAgo.isNotEmpty()) {
					Spacer(modifier = Modifier.width(8.dp))
					Text(
						text = timeAgo,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}

			CommentBody(comment)

			if (!comment.isTakenDown && !comment.isPending) {
				// a tall, wide touch target around a small label
				Box(
					contentAlignment = Alignment.CenterStart,
					modifier = Modifier
						.clip(RoundedCornerShape(8.dp))
						.clickable(role = Role.Button) { onReplyClick(comment) }
						.defaultMinSize(minWidth = 56.dp, minHeight = 40.dp)
				) {
					Text(
						text = "Balas",
						style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
		}
	}
}

@Composable
private fun CommentBody(comment: CommentItem) {
	val content = comment.commentContent
	if (comment.isTakenDown && content == null) {
		Text(
			text = "Komentar ini diturunkan oleh admin.",
			style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		return
	}

	Text(
		text = buildAnnotatedString {
			if (comment.replyToUsername != null) {
				withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
					append("@${comment.replyToUsername}")
				}
				append(" ")
			}
			append(content.orEmpty())
		},
		style = MaterialTheme.typography.bodyMedium,
		lineHeight = 20.sp,
		color = if (comment.isTakenDown) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
	)

	// only the author gets here for a taken down comment: the text plus why it was taken down
	if (comment.isTakenDown) {
		Text(
			text = "Diturunkan oleh admin" + comment.takedownReason?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty(),
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.error
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun CommentRowPreview() {
	FeedzmoreTheme {
		Column(modifier = Modifier.padding(16.dp)) {
			CommentRow(
				comment = CommentItem(
					id = "1",
					username = "John Doe",
					commentContent = "This is a comment.",
					createdAt = "2026-11-11T10:00:00",
					replyCount = 2
				),
				onReplyClick = {},
				modifier = Modifier,
				isReplyTarget = false,
				threadLine = ThreadLine.RootStart
			)
			CommentRow(
				comment = CommentItem(
					id = "2",
					username = "Budi",
					commentContent = "Setuju banget!",
					createdAt = "2026-11-11T10:05:00",
					parentId = "1",
					replyToCommentId = "1",
					replyToUsername = "John Doe"
				),
				onReplyClick = {},
				modifier = Modifier,
				isReplyTarget = true,
				threadLine = ThreadLine.Branch
			)
			CommentRow(
				comment = CommentItem(
					id = "3",
					username = "Citra",
					commentContent = null,
					parentId = "1",
					replyToCommentId = "2",
					replyToUsername = "Budi",
					moderationStatus = "TAKEN_DOWN"
				),
				onReplyClick = {},
				modifier = Modifier,
				isReplyTarget = false,
				threadLine = ThreadLine.LastBranch
			)
		}
	}
}
