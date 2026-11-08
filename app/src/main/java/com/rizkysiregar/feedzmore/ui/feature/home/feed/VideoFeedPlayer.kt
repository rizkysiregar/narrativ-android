package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.material3.MaterialTheme
import com.rizkysiregar.feedzmore.ui.theme.Surface2
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.rizkysiregar.feedzmore.media.currentMediaUrlOrNull
import com.rizkysiregar.feedzmore.ui.components.ShimmerEffect

@OptIn(UnstableApi::class)
@Composable
fun VideoFeedPlayer(
	mediaUrl: String,
	thumbnailUrl: String?,
	isActive: Boolean,
	player: ExoPlayer?,
	muted: Boolean = false,
	showMuteButton: Boolean = false,
	onToggleMute: (() -> Unit)? = null,
	modifier: Modifier = Modifier,
	aspectRatio: Float? = 1.08f,
	playerResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
	onVideoClick: (() -> Unit)? = null,
) {
	// Poster stays on top until the very first video frame is rendered,
	// so the user never sees the black PlayerView shutter while buffering.
	var firstFrameRendered by remember(mediaUrl) { mutableStateOf(false) }

	if (isActive && player != null) {
		DisposableEffect(player, mediaUrl) {
			val listener = object : Player.Listener {
				override fun onRenderedFirstFrame() {
					if (player.currentMediaUrlOrNull() == mediaUrl) {
						firstFrameRendered = true
					}
				}
			}
			player.addListener(listener)
			// Returning to an already-prepared item (e.g. back from detail):
			// the frame is already on screen, drop the poster immediately.
			if (player.playbackState == Player.STATE_READY &&
				player.currentMediaUrlOrNull() == mediaUrl
			) {
				firstFrameRendered = true
			}
			onDispose { player.removeListener(listener) }
		}
	}

	val boxModifier = if (aspectRatio != null) {
		modifier.fillMaxWidth().aspectRatio(aspectRatio)
	} else {
		modifier.fillMaxSize()
	}

	Box(
		modifier = boxModifier.clickable(enabled = onVideoClick != null) {
			onVideoClick?.invoke()
		},
		contentAlignment = Alignment.Center
	) {
		if (isActive && player != null) {
			AndroidView(
				factory = { ctx ->
					PlayerView(ctx).apply {
						this.player = player
						useController = false
						resizeMode = playerResizeMode
						// Transparent shutter: keeps the poster visible behind it
						// instead of flashing a black surface.
						setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
						layoutParams = ViewGroup.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.MATCH_PARENT
						)
					}
				},
				update = { view ->
					if (view.player != player) view.player = player
					if (view.resizeMode != playerResizeMode) view.resizeMode = playerResizeMode
				},
				onRelease = { view -> view.player = null },
				modifier = Modifier.fillMaxSize()
			)
			if (!firstFrameRendered) {
				VideoPoster(
					mediaUrl = mediaUrl,
					thumbnailUrl = thumbnailUrl,
					showPlayIcon = false
				)
			}
		} else {
			VideoPoster(mediaUrl = mediaUrl, thumbnailUrl = thumbnailUrl, showPlayIcon = true)
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
					tint = MaterialTheme.colorScheme.onBackground
				)
			}
		}
	}
}

@Composable
private fun VideoPoster(
	mediaUrl: String,
	thumbnailUrl: String?,
	showPlayIcon: Boolean
) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(VideoPosterFallbackColor)
	) {
		if (thumbnailUrl != null) {
			SubcomposeAsyncImage(
				model = ImageRequest.Builder(LocalContext.current)
					.data(thumbnailUrl)
					.build(),
				contentDescription = "Thumbnail video",
				modifier = Modifier.fillMaxSize(),
				contentScale = ContentScale.Crop,
				loading = { ShimmerEffect(modifier = Modifier.fillMaxSize()) },
				error = {
					Box(modifier = Modifier.fillMaxSize().background(VideoPosterFallbackColor))
				}
			)
		}
		// Without a server thumbnail we intentionally show a plain placeholder.
		// Loading the video URL as an image never works (Coil can't decode MP4)
		// and previously caused wasted requests + flicker.
		if (showPlayIcon) {
			Icon(
				imageVector = Icons.Filled.PlayCircle,
				contentDescription = "Putar video",
				tint = MaterialTheme.colorScheme.onBackground,
				modifier = Modifier
					.align(Alignment.Center)
					.size(64.dp)
			)
		}
	}
}

private val VideoPosterFallbackColor = Surface2
