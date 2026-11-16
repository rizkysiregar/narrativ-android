package com.rizkysiregar.narrativ.ui.feature.home.feed

import com.rizkysiregar.narrativ.ui.theme.Like
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.rizkysiregar.narrativ.ui.feature.moderation.ContentModerationMenu
import com.rizkysiregar.narrativ.ui.feature.moderation.ModerationTarget
import com.rizkysiregar.narrativ.domain.moderation.ReportTarget
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.data.remote.model.response.feed.MediaType
import com.rizkysiregar.narrativ.data.remote.model.response.feed.PostMediaItem
import com.rizkysiregar.narrativ.ui.components.InitialAvatar
import com.rizkysiregar.narrativ.ui.components.TakenDownPlaceholder
import com.rizkysiregar.narrativ.ui.theme.NarrativTheme
import com.rizkysiregar.narrativ.util.formatPostDate
import androidx.compose.ui.res.stringResource

private const val CAPTION_ONLY_MAX_LINES = 10
private const val CAPTION_WITH_MEDIA_MAX_LINES = 4
private val MediaShape = RoundedCornerShape(16.dp)

/**
 * A post, caption first: author, then the caption, then the media below it as an attachment, and
 * the like/comment actions at the bottom. A caption-only post is just the text, like a thread.
 */
@Composable
fun FeedItemCard(
	item: FeedContentItem,
	onLikeClick: (String) -> Unit,
	onCommentClick: (FeedContentItem) -> Unit = {},
	isActiveVideo: Boolean = false,
	isVideoMuted: Boolean = false,
	onToggleVideoMute: () -> Unit = {},
	onVideoClick: (() -> Unit)? = null,
	player: ExoPlayer? = null,
	initialPage: Int = 0,
	onPageChange: (Int) -> Unit = {},
	// set where the card may be reported or its author blocked; runs after the author was blocked
	onAuthorBlocked: (() -> Unit)? = null,
	// set where the author may be opened; receives the author's id and username
	onAuthorClick: ((userId: String, username: String) -> Unit)? = null
) {
	val likeScale by animateFloatAsState(
		targetValue = if (item.liked) 1.12f else 1f,
		animationSpec = tween(durationMillis = 180),
		label = "feed-like-scale"
	)
	val likeColor by animateColorAsState(
		targetValue = if (item.liked) Like else MaterialTheme.colorScheme.onSurfaceVariant,
		animationSpec = tween(durationMillis = 180),
		label = "feed-like-color"
	)

	Card(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(20.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
		elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
	) {
		Column(modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 6.dp)) {
			FeedAuthorHeader(item = item, onAuthorBlocked = onAuthorBlocked, onAuthorClick = onAuthorClick)

			if (item.isTakenDown) {
				Spacer(Modifier.height(12.dp))
				TakenDownPlaceholder(
					message = item.takedownMessage,
					modifier = Modifier
						.fillMaxWidth()
						.aspectRatio(1.6f)
						.clip(MediaShape)
				)
			} else {
				if (!item.caption.isNullOrBlank()) {
					Spacer(Modifier.height(10.dp))
					ExpandableCaption(
						caption = item.caption,
						captionOnly = !item.hasMedia
					)
				}
				if (item.hasMedia) {
					Spacer(Modifier.height(12.dp))
					PostMediaPager(
						media = item.mediaItems,
						isActive = isActiveVideo,
						player = player,
						muted = isVideoMuted,
						initialPage = initialPage,
						onPageChange = onPageChange,
						onToggleMute = onToggleVideoMute,
						onVideoClick = onVideoClick,
						aspectRatio = 1f,
						modifier = Modifier.clip(MediaShape)
					)
				}
			}

			FeedActions(
				item = item,
				likeScale = likeScale,
				likeColor = likeColor,
				onLikeClick = { onLikeClick(item.id) },
				onCommentClick = { onCommentClick(item) }
			)
		}
	}
}

@Composable
private fun FeedAuthorHeader(
	item: FeedContentItem,
	onAuthorBlocked: (() -> Unit)?,
	onAuthorClick: ((userId: String, username: String) -> Unit)?
) {
	// an older server may not send the author's id, then there is nowhere to go
	val openAuthor = item.userId?.let { id -> onAuthorClick?.let { { it(id, item.username) } } }
	val authorClick = if (openAuthor != null) {
		Modifier.clickable(
			interactionSource = remember { MutableInteractionSource() },
			indication = null,
			onClick = openAuthor
		)
	} else {
		Modifier
	}

	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically
	) {
		InitialAvatar(name = item.username, size = 38.dp, modifier = authorClick)
		Spacer(Modifier.width(10.dp))
		Text(
			text = item.username,
			style = MaterialTheme.typography.titleSmall,
			fontWeight = FontWeight.SemiBold,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.weight(1f, fill = false).then(authorClick)
		)
		if (item.createdAt.isNotBlank()) {
			Text(
				text = "  ·  ${formatPostDate(item.createdAt)}",
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				maxLines = 1
			)
		}
		if (onAuthorBlocked != null) {
			Spacer(Modifier.weight(1f))
			ContentModerationMenu(
				target = ModerationTarget(ReportTarget.POST, item.id, item.userId, item.username),
				onBlocked = onAuthorBlocked
			)
		}
	}
}

/**
 * The caption, cut after a few lines with a "see more" toggle. A caption-only post gets a larger
 * type and more lines since the text is the whole post.
 */
@Composable
private fun ExpandableCaption(caption: String, captionOnly: Boolean) {
	var expanded by rememberSaveable(caption) { mutableStateOf(false) }
	var overflows by remember(caption) { mutableStateOf(false) }
	val maxLines = if (captionOnly) CAPTION_ONLY_MAX_LINES else CAPTION_WITH_MEDIA_MAX_LINES

	Column(
		modifier = Modifier
			.animateContentSize()
			.clickable(
				enabled = overflows || expanded,
				interactionSource = remember { MutableInteractionSource() },
				indication = null
			) { expanded = !expanded }
	) {
		Text(
			text = caption,
			style = if (captionOnly) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurface,
			maxLines = if (expanded) Int.MAX_VALUE else maxLines,
			overflow = TextOverflow.Ellipsis,
			onTextLayout = { result -> if (!expanded) overflows = result.hasVisualOverflow }
		)
		if (overflows || expanded) {
			Text(
				text = if (expanded) stringResource(R.string.show_less) else stringResource(R.string.show_more),
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(top = 4.dp)
			)
		}
	}
}

@Composable
private fun FeedActions(
	item: FeedContentItem,
	likeScale: Float,
	likeColor: Color,
	onLikeClick: () -> Unit,
	onCommentClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 6.dp),
		horizontalArrangement = Arrangement.spacedBy(4.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		ActionButton(onClick = onLikeClick) {
			Icon(
				imageVector = if (item.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
				contentDescription = if (item.liked) stringResource(R.string.cd_unlike) else stringResource(R.string.cd_like),
				tint = likeColor,
				modifier = Modifier.size(20.dp).scale(likeScale)
			)
			Text(
				text = item.likeCount.toString(),
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}

		ActionButton(onClick = onCommentClick) {
			Icon(
				painter = painterResource(R.drawable.comment_regular_full),
				contentDescription = stringResource(R.string.comments),
				tint = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.size(18.dp)
			)
			Text(
				text = item.commentCount.toString(),
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
private fun ActionButton(onClick: () -> Unit, content: @Composable () -> Unit) {
	Row(
		modifier = Modifier
			.clip(RoundedCornerShape(50))
			.clickable(onClick = onClick)
			.padding(horizontal = 8.dp, vertical = 8.dp),
		horizontalArrangement = Arrangement.spacedBy(6.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		content()
	}
}

@Preview(showBackground = true)
@Composable
private fun FeedItemCardPreview() {
	NarrativTheme {
		FeedItemCard(
			item = FeedContentItem(
				id = "1",
				caption = "Menikmati sore dan mencoba pengalaman baru bersama teman-teman.",
				commentCount = 8,
				likeCount = 124,
				liked = true,
				createdAt = "2026-10-05",
				media = listOf(PostMediaItem("m1", MediaType.IMAGE, "https://dummy-url", null)),
				username = "John Doe"
			),
			onLikeClick = {}
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun FeedItemCardCaptionOnlyPreview() {
	NarrativTheme {
		FeedItemCard(
			item = FeedContentItem(
				id = "2",
				caption = "Kadang yang paling dibutuhkan cuma secangkir kopi dan waktu sendiri.",
				commentCount = 2,
				likeCount = 31,
				liked = false,
				createdAt = "2026-11-02",
				username = "Jane Doe"
			),
			onLikeClick = {}
		)
	}
}
