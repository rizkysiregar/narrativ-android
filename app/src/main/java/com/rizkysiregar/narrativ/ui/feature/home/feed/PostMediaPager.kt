package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.rizkysiregar.narrativ.data.remote.model.response.feed.PostMediaItem
import com.rizkysiregar.narrativ.ui.components.ShimmerEffect
import com.rizkysiregar.narrativ.R
import androidx.compose.ui.res.stringResource

/**
 * The media of one post: a single image or video is drawn as is, several are swipeable with a
 * page counter and dots. Only the video on the settled page is active, and the host is told
 * which page that is ([onPageChange]) so it can load that video into its shared player.
 * [initialPage] only seeds the pager (e.g. when the item scrolls back into a list).
 */
@OptIn(ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PostMediaPager(
	media: List<PostMediaItem>,
	isActive: Boolean,
	player: ExoPlayer?,
	muted: Boolean,
	modifier: Modifier = Modifier,
	initialPage: Int = 0,
	onPageChange: (Int) -> Unit = {},
	onToggleMute: (() -> Unit)? = null,
	// a tap on a page (photo or video) with its index; null leaves taps to the page itself
	onMediaClick: ((page: Int) -> Unit)? = null,
	aspectRatio: Float? = 1.08f,
	imageContentScale: ContentScale = ContentScale.Crop,
	videoResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
	showMuteButton: Boolean = true
) {
	if (media.isEmpty()) return

	val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, media.lastIndex)) { media.size }
	val currentOnPageChange by rememberUpdatedState(onPageChange)
	LaunchedEffect(pagerState) {
		snapshotFlow { pagerState.settledPage }.collect { currentOnPageChange(it) }
	}

	val boxModifier = if (aspectRatio != null) {
		modifier.fillMaxWidth().aspectRatio(aspectRatio)
	} else {
		modifier.fillMaxSize()
	}

	Box(modifier = boxModifier) {
		HorizontalPager(
			state = pagerState,
			beyondViewportPageCount = 0,
			modifier = Modifier.fillMaxSize(),
			key = { media[it].id }
		) { page ->
			PostMediaPage(
				item = media[page],
				isActive = isActive && page == pagerState.settledPage,
				player = player,
				muted = muted,
				onToggleMute = onToggleMute,
				onClick = onMediaClick?.let { click -> { click(page) } },
				imageContentScale = imageContentScale,
				videoResizeMode = videoResizeMode,
				showMuteButton = showMuteButton
			)
		}

		if (media.size > 1) {
			PageCounter(
				current = pagerState.currentPage + 1,
				total = media.size,
				modifier = Modifier
					.align(Alignment.TopEnd)
					.padding(10.dp)
			)
			PageDots(
				current = pagerState.currentPage,
				total = media.size,
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.padding(bottom = 10.dp)
			)
		}
	}
}

@Composable
private fun PostMediaPage(
	item: PostMediaItem,
	isActive: Boolean,
	player: ExoPlayer?,
	muted: Boolean,
	onToggleMute: (() -> Unit)?,
	onClick: (() -> Unit)?,
	imageContentScale: ContentScale,
	videoResizeMode: Int,
	showMuteButton: Boolean
) {
	if (item.isVideo) {
		VideoFeedPlayer(
			mediaUrl = item.url.orEmpty(),
			thumbnailUrl = item.thumbnailUrl,
			isActive = isActive,
			player = player,
			muted = muted,
			showMuteButton = showMuteButton,
			onToggleMute = onToggleMute,
			onVideoClick = onClick,
			modifier = Modifier.fillMaxSize(),
			aspectRatio = null,
			playerResizeMode = videoResizeMode
		)
	} else {
		PostMediaImage(
			imageUrl = item.url.orEmpty(),
			contentScale = imageContentScale,
			modifier = Modifier
				.fillMaxSize()
				.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
		)
	}
}

@Composable
fun PostMediaImage(
	imageUrl: String,
	contentScale: ContentScale,
	modifier: Modifier = Modifier
) {
	SubcomposeAsyncImage(
		model = ImageRequest.Builder(LocalContext.current).data(imageUrl).build(),
		contentDescription = stringResource(R.string.cd_feed_photo),
		modifier = modifier,
		contentScale = contentScale,
		loading = { ShimmerEffect(modifier = Modifier.fillMaxSize()) },
		error = {
			Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				Icon(
					imageVector = Icons.Outlined.Info,
					contentDescription = stringResource(R.string.cd_image_failed),
					tint = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.size(32.dp)
				)
			}
		}
	)
}

@Composable
private fun PageCounter(current: Int, total: Int, modifier: Modifier = Modifier) {
	Surface(
		modifier = modifier,
		shape = RoundedCornerShape(50),
		color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
	) {
		Text(
			text = "$current/$total",
			style = MaterialTheme.typography.labelMedium,
			color = MaterialTheme.colorScheme.onBackground,
			modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
		)
	}
}

@Composable
private fun PageDots(current: Int, total: Int, modifier: Modifier = Modifier) {
	Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
		repeat(total) { index ->
			Box(
				modifier = Modifier
					.size(if (index == current) 8.dp else 6.dp)
					.clip(CircleShape)
					.background(
						if (index == current) MaterialTheme.colorScheme.onBackground
						else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
					)
			)
		}
	}
}
