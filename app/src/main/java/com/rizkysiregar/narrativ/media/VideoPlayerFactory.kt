package com.rizkysiregar.narrativ.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.EntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@OptIn(UnstableApi::class)
class VideoPlayerFactory @Inject constructor(
	@ApplicationContext private val context: Context,
	private val cacheManager: VideoCacheManager
) {

	/** Main playback player (repeat-one, starts muted like TikTok-style feeds). */
	fun create(): ExoPlayer = ExoPlayer.Builder(context)
		.setMediaSourceFactory(DefaultMediaSourceFactory(cacheManager.dataSourceFactory))
		.build()
		.apply {
			repeatMode = Player.REPEAT_MODE_ONE
			volume = 0f
		}

	/**
	 * Small-buffer player used to warm the video cache for upcoming items.
	 * It never renders; it only downloads ahead so the shared cache already
	 * contains the next video when the user scrolls to it.
	 */
	fun createPreload(): ExoPlayer = ExoPlayer.Builder(context)
		.setMediaSourceFactory(DefaultMediaSourceFactory(cacheManager.dataSourceFactory))
		.setLoadControl(
			DefaultLoadControl.Builder()
				.setBufferDurationsMs(
					MIN_BUFFER_MS,
					MAX_BUFFER_MS,
					BUFFER_FOR_PLAYBACK_MS,
					BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
				)
				.build()
		)
		.build()
		.apply { volume = 0f }

	private companion object {
		const val MIN_BUFFER_MS = 2_500
		const val MAX_BUFFER_MS = 15_000
		const val BUFFER_FOR_PLAYBACK_MS = 1_500
		const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2_000
	}
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface VideoPlayerFactoryEntryPoint {
	fun videoPlayerFactory(): VideoPlayerFactory
}

/**
 * Creates a cached-backed player owned by the current composition.
 * The player is released automatically when the caller leaves composition.
 * Set [preload] = true to get the small-buffer cache-warming player.
 */
@Composable
fun rememberVideoPlayer(preload: Boolean = false): ExoPlayer {
	val context = LocalContext.current
	val factory = remember(context) {
		EntryPointAccessors.fromApplication(
			context.applicationContext,
			VideoPlayerFactoryEntryPoint::class.java
		).videoPlayerFactory()
	}
	val player = remember { if (preload) factory.createPreload() else factory.create() }
	DisposableEffect(player) {
		onDispose { player.release() }
	}
	return player
}

/** Current media URI as string, or null when nothing is prepared. */
@OptIn(UnstableApi::class)
fun Player.currentMediaUrlOrNull(): String? =
	currentMediaItem?.localConfiguration?.uri?.toString()
