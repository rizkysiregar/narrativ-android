package com.rizkysiregar.narrativ.ui.feature.home.feed

import com.rizkysiregar.narrativ.ui.theme.Like
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.NavController
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.media.currentMediaUrlOrNull
import com.rizkysiregar.narrativ.media.rememberVideoPlayer
import androidx.compose.ui.res.stringResource

/**
 * A post's media full screen, with its like and comment buttons beside it. The comments live in the post's thread,
 * so the comment button and the caption open that ([onCommentsClick]).
 */
@Composable
fun PostMediaViewerDestination(
	navController: NavController,
	postId: String,
	initialPage: Int,
	handoff: PostDetailHandoff,
	onCommentsClick: () -> Unit,
	viewModel: PostDetailViewModel = hiltViewModel()
) {
	// Opened from a post list or a thread: that screen hands the post over, so it shows at once. After the process
	// was killed there is nothing to hand over, so the post is loaded by its id.
	val handedOver = handoff.postFor(postId)
	val likeHandler = handoff.likeHandlerFor(postId)

	if (handedOver != null && likeHandler != null) {
		PostMediaViewerScreen(
			post = handedOver,
			initialPage = initialPage,
			onBackClick = { navController.popBackStack() },
			onLikeClick = likeHandler,
			onCommentsClick = onCommentsClick
		)
		return
	}

	LaunchedEffect(postId) { viewModel.load(postId) }
	val state by viewModel.state.collectAsStateWithLifecycle()
	when (val current = state) {
		is PostDetailUiState.Loaded -> PostMediaViewerScreen(
			post = current.post,
			initialPage = initialPage,
			onBackClick = { navController.popBackStack() },
			onLikeClick = viewModel::toggleLike,
			onCommentsClick = onCommentsClick
		)

		PostDetailUiState.Loading -> PostDetailMessage(
			onBackClick = { navController.popBackStack() }
		) { CircularProgressIndicator() }

		PostDetailUiState.Unavailable -> PostDetailMessage(
			onBackClick = { navController.popBackStack() }
		) {
			Text(stringResource(R.string.post_not_found), color = MaterialTheme.colorScheme.onBackground)
		}

		is PostDetailUiState.Error -> PostDetailMessage(
			onBackClick = { navController.popBackStack() }
		) {
			Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
				Text(current.message, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
				TextButton(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) }
			}
		}
	}
}

/** A centered message (spinner, "not found", retry) with a back button, for when there is no post to show. */
@Composable
internal fun PostDetailMessage(
	onBackClick: () -> Unit,
	content: @Composable () -> Unit
) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		IconButton(
			onClick = onBackClick,
			modifier = Modifier
				.align(Alignment.TopStart)
				.statusBarsPadding()
				.padding(8.dp)
		) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.ArrowBack,
				contentDescription = stringResource(R.string.back),
				tint = MaterialTheme.colorScheme.onBackground
			)
		}
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 32.dp),
			contentAlignment = Alignment.Center
		) { content() }
	}
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PostMediaViewerScreen(
	post: FeedContentItem,
	onBackClick: () -> Unit,
	onLikeClick: (String) -> Unit,
	onCommentsClick: () -> Unit,
	initialPage: Int = 0
) {
	var liked by remember { mutableStateOf(post.liked) }
	var likeCount by remember { mutableIntStateOf(post.likeCount) }
	var isMuted by remember { mutableStateOf(false) }

	val videoPlayer = rememberVideoPlayer()

	// a post can hold several media: the video on the page being shown plays, an image page pauses it
	var page by remember { mutableIntStateOf(initialPage) }
	val activeVideo = post.videoAt(page)

	LaunchedEffect(activeVideo?.url, isMuted) {
		if (activeVideo == null) {
			videoPlayer.playWhenReady = false
			return@LaunchedEffect
		}
		val currentUri = videoPlayer.currentMediaUrlOrNull()
		videoPlayer.volume = if (isMuted) 0f else 1f
		if (currentUri != activeVideo.url.orEmpty()) {
			videoPlayer.setMediaItem(MediaItem.fromUri(activeVideo.url.orEmpty()))
			videoPlayer.prepare()
		}
		videoPlayer.playWhenReady = true
	}

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		if (post.hasMedia) {
			PostMediaPager(
				media = post.mediaItems,
				isActive = true,
				player = videoPlayer,
				muted = isMuted,
				initialPage = initialPage,
				onPageChange = { page = it },
				showMuteButton = false,
				// aspectRatio = null -> the pager fills the whole screen; videos letterbox
				// (RESIZE_MODE_FIT) and images fit, so they are shown as large as possible.
				aspectRatio = null,
				imageContentScale = ContentScale.Fit,
				videoResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
				modifier = Modifier.fillMaxSize()
			)
		} else {
			// caption-only post: the text is the content
			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState())
					.padding(start = 16.dp, end = 84.dp, top = 72.dp, bottom = 24.dp)
			) {
				Text(
					text = post.username,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onBackground
				)
				Spacer(Modifier.height(12.dp))
				Text(
					text = post.caption.orEmpty(),
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onBackground
				)
			}
		}

		IconButton(
			onClick = onBackClick,
			modifier = Modifier
				.align(Alignment.TopStart)
				.statusBarsPadding()
				.padding(8.dp)
		) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.ArrowBack,
				contentDescription = stringResource(R.string.back),
				tint = MaterialTheme.colorScheme.onBackground
			)
		}

		if (activeVideo != null) {
			IconButton(
				onClick = { isMuted = !isMuted },
				modifier = Modifier
					.align(Alignment.TopEnd)
					.statusBarsPadding()
					.padding(8.dp)
			) {
				Icon(
					imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
					contentDescription = stringResource(if (isMuted) R.string.unmute else R.string.mute),
					tint = MaterialTheme.colorScheme.onBackground
				)
			}
		}

		Column(
			modifier = Modifier
				.align(Alignment.CenterEnd)
				.padding(end = 10.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(22.dp)
		) {
			ActionRailItem(
				icon = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
				count = likeCount,
				tint = if (liked) Like else MaterialTheme.colorScheme.onBackground,
				onClick = {
					liked = !liked
					likeCount = if (liked) likeCount + 1 else likeCount - 1
					onLikeClick(post.id)
				}
			)
			ActionRailItem(
				painter = painterResource(R.drawable.comment_regular_full),
				count = post.commentCount,
				tint = MaterialTheme.colorScheme.onBackground,
				onClick = onCommentsClick
			)
		}

		if (post.hasMedia && !post.caption.isNullOrBlank()) {
			Text(
				text = buildAnnotatedString {
					withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
						append(post.username)
					}
					append("  ")
					append(post.caption.orEmpty())
				},
				color = MaterialTheme.colorScheme.onBackground,
				style = MaterialTheme.typography.bodyMedium,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier
					.align(Alignment.BottomStart)
					.navigationBarsPadding()
					.padding(start = 12.dp, end = 84.dp, bottom = 14.dp)
					.clickable(onClick = onCommentsClick)
			)
		}
	}
}

@Composable
private fun ActionRailItem(
	icon: ImageVector? = null,
	painter: androidx.compose.ui.graphics.painter.Painter? = null,
	count: Int,
	tint: Color,
	onClick: () -> Unit
) {
	Column(horizontalAlignment = Alignment.CenterHorizontally) {
		IconButton(onClick = onClick) {
			if (icon != null) {
				Icon(icon, contentDescription = null, tint = tint)
			} else if (painter != null) {
				Icon(painter, contentDescription = null, tint = tint)
			}
		}
		Text(
			text = count.toString(),
			color = MaterialTheme.colorScheme.onBackground,
			style = MaterialTheme.typography.labelMedium
		)
	}
}
