package com.rizkysiregar.feedzmore.ui.feature.home.feed

import com.rizkysiregar.feedzmore.ui.theme.Like
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.rizkysiregar.feedzmore.R
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.MediaType
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.PostMediaItem
import com.rizkysiregar.feedzmore.ui.components.InitialAvatar
import com.rizkysiregar.feedzmore.ui.components.ShimmerEffect
import com.rizkysiregar.feedzmore.ui.components.TakenDownPlaceholder
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

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
	onPageChange: (Int) -> Unit = {}
) {
	val likeScale by animateFloatAsState(
		targetValue = if (item.liked) 1.12f else 1f,
		animationSpec = tween(durationMillis = 180),
		label = "feed-like-scale"
	)
	val likeColor by animateColorAsState(
		targetValue = if (item.liked) Like else MaterialTheme.colorScheme.onSurface,
		animationSpec = tween(durationMillis = 180),
		label = "feed-like-color"
	)

	Card(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(22.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column {
			FeedAuthorHeader(item = item)
		if (item.isTakenDown) {
			TakenDownPlaceholder(
				message = item.takedownMessage,
				modifier = Modifier
					.fillMaxWidth()
					.aspectRatio(1.08f)
			)
		} else {
		if (item.hasMedia) {
			PostMediaPager(
				media = item.mediaItems,
				isActive = isActiveVideo,
				player = player,
				muted = isVideoMuted,
				initialPage = initialPage,
				onPageChange = onPageChange,
				onToggleMute = onToggleVideoMute,
				onVideoClick = onVideoClick
			)
		} else if (!item.caption.isNullOrBlank()) {
			// caption-only post: the text is the content, so it comes before the actions
			Text(
				text = item.caption,
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
				maxLines = 12,
				overflow = TextOverflow.Ellipsis
			)
		}
			FeedActions(
				item = item,
				likeScale = likeScale,
				likeColor = likeColor,
				onLikeClick = { onLikeClick(item.id) },
				onCommentClick = { onCommentClick(item) }
			)
			AnimatedVisibility(visible = item.hasMedia && !item.caption.isNullOrBlank()) {
				Text(
					text = buildAnnotatedString {
						withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
							append(item.username)
						}
						append("  ")
						append(item.caption.orEmpty())
					},
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
					maxLines = 3,
					overflow = TextOverflow.Ellipsis
				)
			}
		}
		}
	}
}

@Composable
private fun FeedAuthorHeader(item: FeedContentItem) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 13.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		InitialAvatar(name = item.username, size = 42.dp)
		Spacer(Modifier.width(11.dp))
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = item.username,
				style = MaterialTheme.typography.titleSmall,
				fontWeight = FontWeight.SemiBold,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
			if (item.createdAt.isNotBlank()) {
				Text(
					text = item.createdAt.toReadableDate(),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
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
			.padding(horizontal = 14.dp, vertical = 12.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		ActionChip(onClick = onLikeClick) {
			Icon(
				imageVector = if (item.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
				contentDescription = if (item.liked) "Batalkan suka" else "Suka",
				tint = likeColor,
				modifier = Modifier.size(20.dp).scale(likeScale)
			)
			Text(item.likeCount.toString(), style = MaterialTheme.typography.labelLarge)
		}

		ActionChip(onClick = onCommentClick) {
			Icon(
				painter = painterResource(R.drawable.comment_regular_full),
				contentDescription = "Komentar",
				modifier = Modifier.size(19.dp)
			)
			Text(item.commentCount.toString(), style = MaterialTheme.typography.labelLarge)
		}
	}
}

@Composable
private fun ActionChip(onClick: () -> Unit, content: @Composable () -> Unit) {
	Surface(
		onClick = onClick,
		shape = RoundedCornerShape(50),
		color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
	) {
		Row(
			modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
			horizontalArrangement = Arrangement.spacedBy(7.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			content()
		}
	}
}

private fun String.toReadableDate(): String =
	take(10).replace('-', '·')

@Preview(showBackground = true)
@Composable
private fun FeedItemCardPreview() {
	FeedzmoreTheme {
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
