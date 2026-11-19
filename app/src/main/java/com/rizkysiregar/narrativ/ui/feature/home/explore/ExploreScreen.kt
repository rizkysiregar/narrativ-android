package com.rizkysiregar.narrativ.ui.feature.home.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.ui.feature.home.feed.FeedUiState
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostViewMode
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostViewPager
import com.rizkysiregar.narrativ.ui.feature.home.search.SearchResults
import com.rizkysiregar.narrativ.ui.feature.home.search.SearchUserBar
import com.rizkysiregar.narrativ.ui.feature.home.search.SearchUserUiState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExploreScreenContent(
	pagerState: PagerState,
	gridFeed: FeedUiState,
	listFeed: FeedUiState,
	onPostClick: (FeedContentItem) -> Unit,
	onLikeClick: (String) -> Unit,
	onCommentClick: (FeedContentItem) -> Unit,
	onMediaClick: (FeedContentItem, page: Int) -> Unit,
	onLoadMoreGrid: () -> Unit,
	onLoadMoreList: () -> Unit,
	onRetryGrid: () -> Unit,
	onRetryList: () -> Unit,
	searchState: SearchUserUiState,
	onSearchQueryChange: (String) -> Unit,
	onSearchRetry: () -> Unit,
	onSearchLoadMore: () -> Unit,
	onUserClick: (userId: String, username: String) -> Unit,
	onRefresh: () -> Unit = {},
	onAuthorBlocked: (() -> Unit)? = null
) {
	val isSearching = searchState.query.isNotBlank()

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
					onPostClick = onPostClick,
					onLikeClick = onLikeClick,
					onCommentClick = onCommentClick,
					onMediaClick = onMediaClick,
					onLoadMoreGrid = onLoadMoreGrid,
					onLoadMoreList = onLoadMoreList,
					onRetryGrid = onRetryGrid,
					onRetryList = onRetryList,
					modifier = Modifier.fillMaxSize(),
					onAuthorBlocked = onAuthorBlocked,
					onAuthorClick = { userId, username ->
						onUserClick(userId, username)
					}
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
	}
}
