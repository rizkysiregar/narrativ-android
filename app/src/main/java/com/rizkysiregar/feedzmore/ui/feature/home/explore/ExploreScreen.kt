package com.rizkysiregar.feedzmore.ui.feature.home.explore

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedItemCard
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedUiState
import com.rizkysiregar.feedzmore.ui.feature.home.feed.PostViewMode
import com.rizkysiregar.feedzmore.ui.feature.home.feed.PostViewPager
import com.rizkysiregar.feedzmore.ui.feature.home.search.SearchResults
import com.rizkysiregar.feedzmore.ui.feature.home.search.SearchUserBar
import com.rizkysiregar.feedzmore.ui.feature.home.search.SearchUserUiState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExploreScreenContent(
	pagerState: PagerState,
	gridFeed: FeedUiState,
	listFeed: FeedUiState,
	selectedPost: FeedContentItem?,
	onGridPostClick: (FeedContentItem) -> Unit,
	onDismissDetail: () -> Unit,
	onLikeClick: (String) -> Unit,
	onCommentClick: (FeedContentItem) -> Unit,
	onVideoClick: (FeedContentItem) -> Unit,
	onLoadMoreGrid: () -> Unit,
	onLoadMoreList: () -> Unit,
	onRetryGrid: () -> Unit,
	onRetryList: () -> Unit,
	searchState: SearchUserUiState,
	onSearchQueryChange: (String) -> Unit,
	onSearchRetry: () -> Unit,
	onSearchLoadMore: () -> Unit,
	onUserClick: (userId: String, username: String) -> Unit,
	onRefresh: () -> Unit = {}
) {
	val isSearching = searchState.query.isNotBlank()

	val pagerModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && selectedPost != null) {
		Modifier.fillMaxSize().blur(14.dp)
	} else {
		Modifier.fillMaxSize()
	}

	// the indicator follows the page on screen; a pull refreshes both
	val isRefreshing = if (pagerState.currentPage == PostViewMode.GRID.ordinal) {
		gridFeed.isRefreshing
	} else {
		listFeed.isRefreshing
	}
	val pullState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			SearchUserBar(
				query = searchState.query,
				onQueryChange = onSearchQueryChange,
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 8.dp)
			)

			Box(
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					// a pull inside the results must not refresh the explore feed underneath
					.pullRefresh(pullState, enabled = !isSearching)
			) {
				PostViewPager(
					pagerState = pagerState,
					gridFeed = gridFeed,
					listFeed = listFeed,
					onGridPostClick = onGridPostClick,
					onLikeClick = onLikeClick,
					onVideoClick = onVideoClick,
					onLoadMoreGrid = onLoadMoreGrid,
					onLoadMoreList = onLoadMoreList,
					onRetryGrid = onRetryGrid,
					onRetryList = onRetryList,
					modifier = pagerModifier
				)

				if (isSearching) {
					SearchResults(
						uiState = searchState,
						onRetry = onSearchRetry,
						onLoadMore = onSearchLoadMore,
						onUserClick = onUserClick,
						modifier = Modifier.imePadding()
					)
				} else {
					PullRefreshIndicator(
						refreshing = isRefreshing,
						state = pullState,
						modifier = Modifier.align(Alignment.TopCenter)
					)
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
						// posts with a video open the full screen detail instead, so this card holds images only
						isActiveVideo = false
					)
				}
			}
		}
	}
}
