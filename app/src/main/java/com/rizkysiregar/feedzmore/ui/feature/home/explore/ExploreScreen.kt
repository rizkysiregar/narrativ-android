package com.rizkysiregar.feedzmore.ui.feature.home.explore

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.MediaType
import com.rizkysiregar.feedzmore.ui.components.ShimmerEffect
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedItemCard
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedUiState
import com.rizkysiregar.feedzmore.ui.feature.home.feed.VideoFeedPlayer

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExploreScreenContent(
	uiState: FeedUiState,
	gridState: LazyGridState,
	selectedPost: FeedContentItem?,
	onPostClick: (FeedContentItem) -> Unit,
	onDismissDetail: () -> Unit,
	onLikeClick: (String) -> Unit,
	onCommentClick: (FeedContentItem) -> Unit,
	onRefresh: () -> Unit = {}
) {
	val gridModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && selectedPost != null) {
		Modifier.fillMaxSize().blur(14.dp)
	} else {
		Modifier.fillMaxSize()
	}

	var visibleItemIds by remember { mutableStateOf<Set<String>>(emptySet()) }
	var isDetailVideoMuted by remember { mutableStateOf(false) }

	LaunchedEffect(gridState, uiState.items) {
		snapshotFlow { gridState.layoutInfo }.collect { layoutInfo ->
			visibleItemIds = layoutInfo.visibleItemsInfo
				.mapNotNull { info -> uiState.items.getOrNull(info.index)?.id }
				.toSet()
		}
	}

	val pullState = rememberPullRefreshState(refreshing = uiState.isRefreshing, onRefresh = onRefresh)

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
			.pullRefresh(pullState)
	) {
		LazyVerticalGrid(
			state = gridState,
			columns = GridCells.Fixed(3),
			horizontalArrangement = Arrangement.spacedBy(2.dp),
			verticalArrangement = Arrangement.spacedBy(2.dp),
			modifier = gridModifier
		) {
			items(uiState.items, key = { it.id }) { post ->
				Surface(
					modifier = Modifier
						.fillMaxWidth()
						.aspectRatio(1f)
						.clickable { onPostClick(post) }
				) {
					if (post.mediaType == MediaType.VIDEO) {
						VideoFeedPlayer(
							mediaUrl = post.mediaUrl,
							thumbnailUrl = post.thumbnailUrl,
							autoPlay = post.id in visibleItemIds,
							loop = true,
							muted = true,
							showMuteButton = false,
							aspectRatio = 1f,
							enableTapTogglePause = false
						)
					} else {
						SubcomposeAsyncImage(
							model = ImageRequest.Builder(LocalContext.current)
								.data(post.mediaUrl).build(),
							contentDescription = "Post image",
							contentScale = ContentScale.Crop,
							modifier = Modifier.fillMaxSize(),
							loading = { ShimmerEffect(modifier = Modifier.fillMaxSize()) }
						)
					}
				}
			}
		}

		if (selectedPost != null) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
					.clickable { onDismissDetail() },
				contentAlignment = Alignment.Center
			) {
				Surface(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp)
						.clickable(enabled = true) { },
					shape = RoundedCornerShape(20.dp),
					color = MaterialTheme.colorScheme.surface,
					tonalElevation = 6.dp,
					shadowElevation = 8.dp
				) {
				FeedItemCard(
					item = selectedPost,
					onLikeClick = onLikeClick,
					onCommentClick = onCommentClick,
					shouldPlayVideo = true,
					isVideoMuted = isDetailVideoMuted,
					onToggleVideoMute = { isDetailVideoMuted = !isDetailVideoMuted }
				)
				}
			}
		}

		PullRefreshIndicator(
			refreshing = uiState.isRefreshing,
			state = pullState,
			modifier = Modifier.align(Alignment.TopCenter)
		)
	}
}
