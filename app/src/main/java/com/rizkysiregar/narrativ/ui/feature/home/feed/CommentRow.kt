package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
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
import com.rizkysiregar.narrativ.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.narrativ.ui.components.ProfileAvatar
import com.rizkysiregar.narrativ.ui.theme.NarrativTheme
import com.rizkysiregar.narrativ.domain.moderation.ReportTarget
import com.rizkysiregar.narrativ.ui.feature.moderation.ContentModerationMenu
import com.rizkysiregar.narrativ.ui.feature.moderation.ModerationTarget
import com.rizkysiregar.narrativ.util.formatTimeAgo
import androidx.compose.ui.res.stringResource
import com.rizkysiregar.narrativ.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 08/03/26
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
	isReplyTarget: Boolean = false,
	// set where the comment may be reported or its author blocked; runs after the author was blocked
	onAuthorBlocked: (() -> Unit)? = null
) {
	CommentRow(comment, onReplyClick, modifier, isReplyTarget, ThreadLine.None, onAuthorBlocked)
}

@Composable
internal fun CommentRow(
	comment: CommentItem,
	onReplyClick: (CommentItem) -> Unit,
	modifier: Modifier,
	isReplyTarget: Boolean,
	threadLine: ThreadLine,
	onAuthorBlocked: (() -> Unit)? = null,
	// written by the author of the post: gets the "Author" badge
	isPostAuthor: Boolean = false,
	// the author's own top-level comment as a part of the story (2 for the first one, the post being part 1)
	chapter: Int? = null,
	lineColor: Color = threadLineColor(),
	// the comment a notification pointed at
	isHighlighted: Boolean = false,
	// set for the post's author on a top-level comment: a long press offers to pin or unpin it
	onPinToggle: (() -> Unit)? = null,
	// a tap on the quote above the comment, with the quoted words
	onQuoteClick: (String) -> Unit = {}
) {
	var menuOpen by remember { mutableStateOf(false) }
	val isReply = comment.isReply
	val highlight by animateColorAsState(
		targetValue = when {
			isReplyTarget || isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = if (isHighlighted) 0.14f else 0.08f)
			chapter != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
			else -> Color.Transparent
		},
		label = "replyTargetHighlight"
	)
	val endsThread = threadLine == ThreadLine.None || threadLine == ThreadLine.LastBranch

	Row(
		modifier = modifier
			.fillMaxWidth()
			.alpha(if (comment.isPending) 0.55f else 1f)
			.then(
				if (onPinToggle != null) {
					Modifier.pointerInput(onPinToggle) { detectTapGestures(onLongPress = { menuOpen = true }) }
				} else {
					Modifier
				}
			)
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
			if (comment.pinned) {
				PinnedLabel()
			}
			if (chapter != null) {
				Text(
					text = stringResource(R.string.thread_chapter, chapter),
					style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
					color = MaterialTheme.colorScheme.primary,
					modifier = Modifier.padding(bottom = 2.dp)
				)
			}
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = comment.username,
					style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
				)
				if (isPostAuthor) {
					Spacer(modifier = Modifier.width(6.dp))
					AuthorBadge()
				}
				val timeAgo = if (comment.isPending) stringResource(R.string.sending) else formatTimeAgo(comment.createdAt)
				if (timeAgo.isNotEmpty()) {
					Spacer(modifier = Modifier.width(8.dp))
					Text(
						text = timeAgo,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				if (onAuthorBlocked != null && !comment.isPending) {
					Spacer(modifier = Modifier.weight(1f))
					ContentModerationMenu(
						target = ModerationTarget(ReportTarget.COMMENT, comment.id, comment.userId, comment.username),
						iconSize = 16.dp,
						onBlocked = onAuthorBlocked
					)
				}
			}

			comment.quoteText?.takeIf { it.isNotBlank() && !comment.isTakenDown }?.let { quote ->
				QuoteBlock(text = quote, onClick = { onQuoteClick(quote) })
			}

			CommentBody(comment)

			if (onPinToggle != null) {
				DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
					DropdownMenuItem(
						text = { Text(stringResource(if (comment.pinned) R.string.comment_unpin else R.string.comment_pin)) },
						leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
						onClick = {
							menuOpen = false
							onPinToggle()
						}
					)
				}
			}

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
						text = stringResource(R.string.reply),
						style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
		}
	}
}

@Composable
private fun PinnedLabel() {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier.padding(bottom = 2.dp)
	) {
		Icon(
			imageVector = Icons.Filled.PushPin,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary,
			modifier = Modifier.size(12.dp)
		)
		Spacer(modifier = Modifier.width(4.dp))
		Text(
			text = stringResource(R.string.comment_pinned_label),
			style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
			color = MaterialTheme.colorScheme.primary
		)
	}
}

/** The words of the caption a comment answers, set off by a bar like a quote in a letter. */
@Composable
private fun QuoteBlock(text: String, onClick: () -> Unit) {
	Row(
		modifier = Modifier
			.padding(top = 2.dp, bottom = 4.dp)
			.height(IntrinsicSize.Min)
			.clip(RoundedCornerShape(6.dp))
			.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
			.clickable(onClick = onClick)
	) {
		Box(
			modifier = Modifier
				.width(3.dp)
				.fillMaxHeight()
				.background(MaterialTheme.colorScheme.primary)
		)
		Text(
			text = "“$text”",
			style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			maxLines = 3,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
		)
	}
}

/** Small pill next to the name of the post's author, wherever they comment or reply on their own post. */
@Composable
private fun AuthorBadge() {
	Text(
		text = stringResource(R.string.thread_author_badge),
		style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
		color = MaterialTheme.colorScheme.onPrimary,
		modifier = Modifier
			.clip(RoundedCornerShape(50))
			.background(MaterialTheme.colorScheme.primary)
			.padding(horizontal = 6.dp, vertical = 1.dp)
	)
}

@Composable
private fun CommentBody(comment: CommentItem) {
	val content = comment.commentContent
	if (comment.isTakenDown && content == null) {
		Text(
			text = stringResource(R.string.comment_taken_down),
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
			text = stringResource(R.string.taken_down_by_admin) + comment.takedownReason?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty(),
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.error
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun CommentRowPreview() {
	NarrativTheme {
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
