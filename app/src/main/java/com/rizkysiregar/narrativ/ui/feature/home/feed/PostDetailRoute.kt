package com.rizkysiregar.narrativ.ui.feature.home.feed

import com.rizkysiregar.narrativ.ui.theme.Like
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.media.currentMediaUrlOrNull
import com.rizkysiregar.narrativ.media.rememberVideoPlayer
import com.rizkysiregar.narrativ.ui.components.InitialAvatar
import com.rizkysiregar.narrativ.util.formatPostDate
import androidx.compose.ui.res.stringResource

@Composable
fun PostDetailDestination(
	navController: NavController,
	postId: String,
	openComments: Boolean = false,
	handoff: PostDetailHandoff,
	viewModel: PostDetailViewModel = hiltViewModel()
) {
	// Opened from a post list: that screen hands the post over, so it shows at once. Opened from a push
	// notification (or after the process was killed) there is nothing to hand over, so the post is loaded by its id.
	val handedOver = handoff.postFor(postId)
	val likeHandler = handoff.likeHandlerFor(postId)

	if (handedOver != null && likeHandler != null) {
		PostDetailScreen(
			post = handedOver,
			onBackClick = { navController.popBackStack() },
			onLikeClick = likeHandler,
			openCommentsOnStart = openComments
		)
		return
	}

	LaunchedEffect(postId) { viewModel.load(postId) }
	val state by viewModel.state.collectAsStateWithLifecycle()
	when (val current = state) {
		is PostDetailUiState.Loaded -> PostDetailScreen(
			post = current.post,
			onBackClick = { navController.popBackStack() },
			onLikeClick = viewModel::toggleLike,
			openCommentsOnStart = openComments
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
private fun PostDetailMessage(
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

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PostDetailScreen(
	post: FeedContentItem,
	onBackClick: () -> Unit,
	onLikeClick: (String) -> Unit,
	openCommentsOnStart: Boolean = false
) {
	var liked by remember { mutableStateOf(post.liked) }
	var likeCount by remember { mutableIntStateOf(post.likeCount) }
	var isSheetOpen by remember { mutableStateOf(false) }
	var isMuted by remember { mutableStateOf(false) }

	val videoPlayer = rememberVideoPlayer()

	// a post can hold several media: the video on the page being shown plays, an image page pauses it
	var page by remember { mutableIntStateOf(0) }
	val activeVideo = post.videoAt(page)

	androidx.compose.runtime.LaunchedEffect(activeVideo?.url, isMuted) {
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

	val sheetProgress = remember { Animatable(0f) }
	val scope = rememberCoroutineScope()
	val screenHeightPx = with(LocalDensity.current) {
		LocalConfiguration.current.screenHeightDp.dp.toPx()
	}
	val sheetHeightPx = screenHeightPx * 0.85f

	fun setSheet(open: Boolean) {
		isSheetOpen = open
		scope.launch { sheetProgress.animateTo(if (open) 1f else 0f, tween(durationMillis = 280)) }
	}

	// a push about a comment or reply lands on the post with its comments already open
	androidx.compose.runtime.LaunchedEffect(Unit) {
		if (openCommentsOnStart) setSheet(true)
	}

	val draggableState = rememberDraggableState { delta ->
		scope.launch {
			sheetProgress.snapTo((sheetProgress.value - delta / sheetHeightPx).coerceIn(0f, 1f))
		}
	}

	val videoFraction = 1f - 0.62f * sheetProgress.value
	val showCaptionOverlay = sheetProgress.value < 0.5f

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.fillMaxHeight(videoFraction)
		) {
			if (post.hasMedia) {
				PostMediaPager(
					media = post.mediaItems,
					isActive = true,
					player = videoPlayer,
					muted = isMuted,
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
						.padding(start = 16.dp, end = 84.dp, top = 24.dp, bottom = 24.dp)
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

			if (activeVideo != null) {
				IconButton(
					onClick = { isMuted = !isMuted },
					modifier = Modifier
						.align(Alignment.TopEnd)
						.padding(top = 40.dp, end = 8.dp)
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
					onClick = { setSheet(true) }
				)
			}

			if (post.hasMedia && !post.caption.isNullOrBlank() && showCaptionOverlay) {
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
						.padding(start = 12.dp, end = 84.dp, bottom = 14.dp)
						.clickable { setSheet(true) }
				)
			}
		}

		Surface(
			modifier = Modifier
				.align(Alignment.BottomCenter)
				.fillMaxWidth()
				.fillMaxHeight(0.85f)
				.offset { IntOffset(0, (sheetHeightPx * (1f - sheetProgress.value)).roundToInt()) }
				.draggable(
					state = draggableState,
					orientation = Orientation.Vertical,
					onDragStopped = {
						scope.launch {
							val open = sheetProgress.value > 0.35f
							sheetProgress.animateTo(if (open) 1f else 0f, tween(durationMillis = 250))
							isSheetOpen = open
						}
					}
				),
			color = MaterialTheme.colorScheme.surface,
			shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
		) {
			Column(modifier = Modifier.fillMaxSize()) {
				BottomSheetDefaults.DragHandle(
					modifier = Modifier
						.align(Alignment.CenterHorizontally)
						.padding(top = 10.dp)
				)

				if (!post.caption.isNullOrBlank()) {
					Text(
						text = buildAnnotatedString {
							withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
								append(post.username)
							}
							append("  ")
							append(post.caption.orEmpty())
						},
						style = MaterialTheme.typography.bodyMedium,
						modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)
					)
					HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
				}

				CommentSectionContent(
					postId = post.id,
					modifier = Modifier.weight(1f, fill = true)
				)
			}
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

@Composable
private fun PostDetailHeader(
	post: FeedContentItem,
	onBackClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.background(MaterialTheme.colorScheme.background)
			.padding(horizontal = 8.dp, vertical = 10.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		IconButton(onClick = onBackClick) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.ArrowBack,
				contentDescription = stringResource(R.string.back),
				tint = MaterialTheme.colorScheme.onBackground
			)
		}
		Spacer(Modifier.width(6.dp))
		InitialAvatar(name = post.username, size = 40.dp)
		Spacer(Modifier.width(11.dp))
		Column {
			Text(
				text = post.username,
				style = MaterialTheme.typography.titleSmall,
				fontWeight = FontWeight.SemiBold,
				color = MaterialTheme.colorScheme.onBackground,
				maxLines = 1
			)
			if (post.createdAt.isNotBlank()) {
				Text(
					text = formatPostDate(post.createdAt),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
				)
			}
		}
	}
}
