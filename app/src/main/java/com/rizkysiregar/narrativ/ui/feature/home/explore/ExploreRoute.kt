package com.rizkysiregar.narrativ.ui.feature.home.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostOpener
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostViewMode
import com.rizkysiregar.narrativ.ui.feature.home.search.SearchViewModel

@Composable
fun ExploreRoute(
	viewModel: ExploreViewModel = hiltViewModel(),
	searchViewModel: SearchViewModel = hiltViewModel(),
	postOpener: PostOpener,
	onUserClick: (userId: String, username: String) -> Unit
) {
	val searchState by searchViewModel.uiState.collectAsState()
	val focusManager = LocalFocusManager.current
	val isSearching = searchState.query.isNotBlank()

	// back closes the search first instead of leaving the tab
	BackHandler(enabled = isSearching) {
		searchViewModel.onQueryChange("")
		focusManager.clearFocus()
	}

	val listFeed by viewModel.uiState.collectAsState()
	val gridFeed by viewModel.gridState.collectAsState()
	val pagerState = rememberPagerState(initialPage = PostViewMode.GRID.ordinal) { PostViewMode.entries.size }

	ExploreScreenContent(
		pagerState = pagerState,
		gridFeed = gridFeed,
		listFeed = listFeed,
		onPostClick = { post ->
			if (!post.isTakenDown) postOpener.thread(post, viewModel::toggleLike)
		},
		onLikeClick = viewModel::toggleLike,
		onCommentClick = { post -> postOpener.thread(post, viewModel::toggleLike, openComments = true) },
		onMediaClick = { post, page -> postOpener.media(post, viewModel::toggleLike, page) },
		onLoadMoreGrid = viewModel::loadNextGridPage,
		onLoadMoreList = viewModel::loadNextPage,
		onRetryGrid = {
			if (gridFeed.hasLoadedInitialPage) viewModel.loadNextGridPage() else viewModel.refreshGrid()
		},
		onRetryList = {
			if (listFeed.hasLoadedInitialPage) viewModel.loadNextPage() else viewModel.refresh()
		},
		onRefresh = viewModel::refreshAll,
		onAuthorBlocked = viewModel::refreshAll,
		searchState = searchState,
		onSearchQueryChange = searchViewModel::onQueryChange,
		onSearchRetry = searchViewModel::retry,
		onSearchLoadMore = searchViewModel::loadNextPage,
		onUserClick = onUserClick
	)
}
