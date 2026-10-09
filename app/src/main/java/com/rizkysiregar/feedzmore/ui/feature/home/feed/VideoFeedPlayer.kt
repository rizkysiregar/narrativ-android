package com.rizkysiregar.feedzmore.ui.feature.home.feed

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.rizkysiregar.feedzmore.ui.components.ShimmerEffect

@OptIn(UnstableApi::class)
@Composable
fun VideoFeedPlayer(
	mediaUrl: String,
	thumbnailUrl: String?,
	autoPlay: Boolean = false,
	loop: Boolean = false,
	muted: Boolean = false,
	showMuteButton: Boolean = false,
	onToggleMute: (() -> Unit)? = null,
	modifier: Modifier = Modifier,
	aspectRatio: Float? = 1.08f,
	enableTapTogglePause: Boolean = true,
	onVideoClick: (() -> Unit)? = null,
	playerResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
) {
	var userStarted by remember { mutableStateOf(false) }
	var userPaused by remember { mutableStateOf(false) }

	LaunchedEffect(autoPlay) {
		if (!autoPlay) {
			userStarted = false
			userPaused = false
		}
	}

	val active = autoPlay || userStarted

	Box(
		modifier = if (aspectRatio != null) {
			modifier.fillMaxWidth().aspectRatio(aspectRatio)
		} else {
			modifier.fillMaxSize()
		},
		contentAlignment = Alignment.Center
	) {
		if (active) {
			val context = LocalContext.current
			val player = remember {
				ExoPlayer.Builder(context).build().apply {
					setMediaItem(MediaItem.fromUri(mediaUrl))
					repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
					prepare()
				}
			}
			DisposableEffect(player) {
				onDispose { player.release() }
			}
			LaunchedEffect(userPaused, muted) {
				player.playWhenReady = !userPaused
				player.volume = if (muted) 0f else 1f
			}
			Box(
				modifier = Modifier
					.fillMaxSize()
					.clickable {
						if (onVideoClick != null) onVideoClick()
						else if (enableTapTogglePause) userPaused = !userPaused
					}
			) {
				AndroidView(
					factory = { ctx ->
						PlayerView(ctx).apply {
							this.player = player
							useController = false
						resizeMode = playerResizeMode
							layoutParams = ViewGroup.LayoutParams(
								ViewGroup.LayoutParams.MATCH_PARENT,
								ViewGroup.LayoutParams.MATCH_PARENT
							)
						}
					},
					modifier = Modifier.fillMaxSize()
				)
				if (userPaused) {
					Icon(
						imageVector = Icons.Filled.PlayCircle,
						contentDescription = "Putar video",
						tint = Color.White,
						modifier = Modifier
							.align(Alignment.Center)
							.size(64.dp)
					)
				}
			}
		} else {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.clickable {
						if (onVideoClick != null) onVideoClick()
						else userStarted = true
					}
			) {
				PosterImage(mediaUrl = mediaUrl, thumbnailUrl = thumbnailUrl)
				Icon(
					imageVector = Icons.Filled.PlayCircle,
					contentDescription = "Putar video",
					tint = Color.White,
					modifier = Modifier
						.align(Alignment.Center)
						.size(64.dp)
				)
			}
		}

		if (showMuteButton && onToggleMute != null) {
			IconButton(
				onClick = onToggleMute,
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(8.dp)
			) {
				Icon(
					imageVector = if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
					contentDescription = if (muted) "Suarakan" else "Bisukan",
					tint = Color.White
				)
			}
		}
	}
}

@Composable
private fun PosterImage(mediaUrl: String, thumbnailUrl: String?) {
	SubcomposeAsyncImage(
		model = ImageRequest.Builder(LocalContext.current)
			.data(thumbnailUrl ?: mediaUrl)
			.build(),
		contentDescription = "Thumbnail video",
		modifier = Modifier.fillMaxSize(),
		contentScale = ContentScale.Crop,
		loading = { ShimmerEffect(modifier = Modifier.fillMaxSize()) },
		error = {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(MaterialTheme.colorScheme.surfaceVariant),
				contentAlignment = Alignment.Center
			) {
				Icon(
					imageVector = Icons.Outlined.Info,
					contentDescription = "Thumbnail gagal dimuat",
					tint = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.size(32.dp)
				)
			}
		}
	)
}
