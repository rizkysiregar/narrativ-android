package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.data.remote.model.response.feed.MediaType
import com.rizkysiregar.narrativ.data.remote.model.response.feed.PostMediaItem
import com.rizkysiregar.narrativ.media.currentMediaUrlOrNull
import com.rizkysiregar.narrativ.media.rememberVideoPlayer
import com.rizkysiregar.narrativ.ui.theme.NarrativTheme
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.rizkysiregar.narrativ.R
import androidx.compose.ui.res.stringResource


/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

private const val PRELOAD_DEBOUNCE_MS = 300L
private const val PRELOAD_AHEAD_COUNT = 3


@OptIn(ExperimentalMaterialApi::class, kotlinx.coroutines.FlowPreview::class)
@Composable
fun FeedScreen(
	uiState: FeedUiState,
	onRefresh: () -> Unit,
	onRetry: () -> Unit,
	listState: LazyListState = rememberLazyListState(),
	onLikeClick: (String) -> Unit,
	onCreateFeed: () -> Unit,
	onSearchClick: () -> Unit = {},
	unreadNotifications: Int = 0,
	onNotificationsClick: () -> Unit = {},
	onAuthorClick: ((userId: String, username: String) -> Unit)? = null,
	player: ExoPlayer? = null,
	onPostClick: (FeedContentItem) -> Unit = {},
	onCommentClick: (FeedContentItem) -> Unit = {},
	onMediaClick: (FeedContentItem, page: Int) -> Unit = { _, _ -> },
	onAuthorBlocked: (() -> Unit)? = null
) {
	val currentOnLikeClick by rememberUpdatedState(onLikeClick)
	val pullState =
		rememberPullRefreshState(refreshing = uiState.isRefreshing, onRefresh = onRefresh)

	var isVideoMuted by remember { mutableStateOf(false) }
	var activeVideoPostId by remember { mutableStateOf<String?>(null) }
	val currentActiveVideoId by rememberUpdatedState(activeVideoPostId)
	// the page each multi-media post is on; also seeds a card's pager when it scrolls back into view
	val pageByPost = remember { mutableStateMapOf<String, Int>() }
	val activePage = pageByPost[activeVideoPostId] ?: 0
	val currentItems by rememberUpdatedState(uiState.items)

	// Activate the centered video. If the player already has the same item
	// prepared (scroll away & back, return from detail), just resume it —
	// buffer and playback position are preserved.
	LaunchedEffect(activeVideoPostId, activePage, isVideoMuted, uiState.items) {
		if (player == null) return@LaunchedEffect
		// the video on the page the centered post is showing; a post can hold images and videos mixed
		val activeVideo = uiState.items
			.firstOrNull { it.id == activeVideoPostId && !it.isTakenDown }
			?.videoAt(activePage)
		if (activeVideo != null) {
			val currentUri = player.currentMediaUrlOrNull()
			player.volume = if (isVideoMuted) 0f else 1f
			if (currentUri != activeVideo.url.orEmpty()) {
				player.setMediaItem(MediaItem.fromUri(activeVideo.url.orEmpty()))
				player.prepare()
			}
			player.playWhenReady = true
		} else {
			// Non-video item centered: pause only, never clear the prepared
			// item — clearing destroyed the buffer on every scroll crossing.
			player.playWhenReady = false
		}
	}

	// Pause on app background, resume when the app comes back with an active video.
	val lifecycleOwner = LocalLifecycleOwner.current
	DisposableEffect(lifecycleOwner, player) {
		val observer = LifecycleEventObserver { _, event ->
			when (event) {
				Lifecycle.Event.ON_PAUSE -> player?.pause()
				Lifecycle.Event.ON_RESUME -> if (currentActiveVideoId != null) player?.play()
				else -> Unit
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}

	// The player is owned by the ViewModel — only pause when Feed leaves
	// composition (e.g. navigating to PostDetail), so returning is seamless.
	DisposableEffect(player) {
		onDispose { player?.pause() }
	}

	// Warm the shared cache for the next upcoming video so switching to it
	// starts instantly from the cache instead of a cold network download.
	val preloadPlayer = rememberVideoPlayer(preload = true)
	var preloadedUrl by remember { mutableStateOf<String?>(null) }
	LaunchedEffect(preloadPlayer) {
		snapshotFlow { currentActiveVideoId }
			.debounce(PRELOAD_DEBOUNCE_MS)
			.collect { activeId ->
				val activeIndex = currentItems.indexOfFirst { it.id == activeId }
				val upcoming = if (activeIndex >= 0) {
					currentItems.drop(activeIndex + 1)
				} else {
					currentItems
				}
				val nextVideo = upcoming
					.take(PRELOAD_AHEAD_COUNT)
					.firstOrNull { it.hasVideo && !it.isTakenDown }
				val url = nextVideo?.mediaItems?.firstOrNull { it.isVideo }?.url
				val playingUri = player?.currentMediaUrlOrNull()
				if (url != null && url != preloadedUrl && url != playingUri) {
					preloadedUrl = url
					preloadPlayer.setMediaItem(MediaItem.fromUri(url))
					preloadPlayer.prepare()
				}
			}
	}

	LaunchedEffect(listState, uiState.items) {
		snapshotFlow { listState.layoutInfo }
			.map { layoutInfo ->
				val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
				val activeInfo = layoutInfo.visibleItemsInfo
					.filter { it.contentType == "feed_item" }
					.minByOrNull { info ->
						kotlin.math.abs((info.offset + info.size / 2) - center)
					}
				val activeItem = activeInfo?.index?.let { idx -> uiState.items.getOrNull(idx - 1) }
				if (activeItem?.hasVideo == true && !activeItem.isTakenDown) activeItem.id else null
			}
			.distinctUntilChanged()
			.collect { id -> activeVideoPostId = id }
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
	) {
	FeedHeader(
		pullProgress = pullState.progress,
		isRefreshing = uiState.isRefreshing,
		onSearchClick = onSearchClick,
		unreadNotifications = unreadNotifications,
		onNotificationsClick = onNotificationsClick
	)
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.weight(1f)
			.pullRefresh(pullState)
	) {
		when {
			uiState.errorMessage != null && uiState.items.isEmpty() -> {
				FeedErrorContent(message = uiState.errorMessage, onRetry = onRetry)
			}

			uiState.hasLoadedInitialPage && uiState.items.isEmpty() && !uiState.isRefreshing -> {
				EmptyFeedContent(onCreateFeed = onCreateFeed)
			}

			else -> {
				LazyColumn(
					state = listState,
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(start = 12.dp, top = 14.dp, end = 12.dp, bottom = 28.dp),
					verticalArrangement = Arrangement.spacedBy(16.dp),
					userScrollEnabled = !uiState.showsSkeleton
				) {
					item(contentType = "feed_header") {
						FeedComposerRow(onClick = onCreateFeed)
					}

					// the first page is on its way: the outline of the posts instead of a blank screen
					if (uiState.showsSkeleton) {
						item(contentType = "feed_skeleton") { FeedSkeleton() }
					}

					items(
						items = uiState.items,
						key = { it.id },
						contentType = { "feed_item" }
					) { item ->
						FeedItemCard(
							item = item,
							onLikeClick = { id -> currentOnLikeClick(id) },
							onCommentClick = onCommentClick,
							onClick = { onPostClick(item) },
							isActiveVideo = item.id == activeVideoPostId,
							isVideoMuted = isVideoMuted,
							onToggleVideoMute = { isVideoMuted = !isVideoMuted },
							onMediaClick = { page -> onMediaClick(item, page) },
							player = player,
							initialPage = pageByPost[item.id] ?: 0,
							onPageChange = { page -> pageByPost[item.id] = page },
							onAuthorBlocked = onAuthorBlocked,
							onAuthorClick = onAuthorClick
						)
					}

					if (uiState.isLoadingMore) {
						item {
							Surface(
								modifier = Modifier.fillMaxWidth(),
								shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
								color = MaterialTheme.colorScheme.surface
							) {
								Box(
									modifier = Modifier.padding(18.dp),
									contentAlignment = Alignment.Center
								) {
									CircularProgressIndicator(modifier = Modifier.height(28.dp))
								}
							}
						}
					}
					if (uiState.isError && uiState.items.isNotEmpty()) {
						item {
							Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
								Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
							}
						}
					}
				}
			}
		}
	}
	}
}

@Composable
private fun EmptyFeedContent(onCreateFeed: () -> Unit) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp)
			.verticalScroll(rememberScrollState()),
		contentAlignment = Alignment.Center
	) {
		Surface(
			shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
			color = MaterialTheme.colorScheme.surface,
			tonalElevation = 6.dp,
			shadowElevation = 8.dp
		) {
			Column(
				modifier = Modifier.padding(24.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = stringResource(R.string.feed_empty_title),
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold
				)
				Spacer(modifier = Modifier.height(8.dp))
				Text(
					text = stringResource(R.string.feed_empty_body),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Spacer(modifier = Modifier.height(16.dp))
				Button(onClick = onCreateFeed) {
					Text(stringResource(R.string.create_new_feed))
				}
			}
		}
	}
}

@Composable
private fun FeedErrorContent(message: String, onRetry: () -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp)
			.verticalScroll(rememberScrollState()),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(text = message)
		Spacer(modifier = Modifier.height(8.dp))
		Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
	}
}

@Preview(showBackground = true)
@Composable
fun FeedScreenPreview() {
	NarrativTheme {
		FeedScreen(
			uiState = FeedUiState(
				items = List(10) { index ->
					FeedContentItem(
						id = index.toString(),
						caption = "This is caption $index",
						commentCount = 1,
						likeCount = 1,
						liked = true,
						createdAt = "2023-11-25",
						media = listOf(PostMediaItem("m$index", MediaType.IMAGE, "https://dummy-url", null)),
						username = "John Doe"
					)
				}
			),
			onRefresh = {},
			onRetry = {},
			listState = rememberLazyListState(),
			onLikeClick = {},
			onCreateFeed = {}
		)
	}
}
