package com.rizkysiregar.narrativ.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the app-wide video byte cache so every ExoPlayer instance (feed, preload,
 * explore, detail) reads/writes the same cached bytes. A video that has been watched
 * or preloaded once can be replayed instantly without re-downloading.
 */
@OptIn(UnstableApi::class)
@Singleton
class VideoCacheManager @Inject constructor(
	@ApplicationContext private val context: Context
) {

	val cache: SimpleCache by lazy {
		SimpleCache(
			File(context.cacheDir, "video_playback_cache"),
			LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES),
			StandaloneDatabaseProvider(context)
		)
	}

	val dataSourceFactory: DataSource.Factory by lazy {
		CacheDataSource.Factory()
			.setCache(cache)
			.setUpstreamDataSourceFactory(
				DefaultHttpDataSource.Factory()
					.setAllowCrossProtocolRedirects(true)
					.setConnectTimeoutMs(HTTP_TIMEOUT_MS)
					.setReadTimeoutMs(HTTP_TIMEOUT_MS)
			)
			.setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
	}

	private companion object {
		const val MAX_CACHE_SIZE_BYTES = 256L * 1024 * 1024
		const val HTTP_TIMEOUT_MS = 15_000
	}
}
