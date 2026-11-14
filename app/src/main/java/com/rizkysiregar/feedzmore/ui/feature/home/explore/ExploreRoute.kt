package com.rizkysiregar.feedzmore.ui.feature.home.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.ui.feature.home.feed.CommentSectionSheet
import com.rizkysiregar.feedzmore.ui.feature.home.feed.KEY_LIKE_HANDLER
import com.rizkysiregar.feedzmore.ui.feature.home.feed.KEY_POST_JSON
import com.rizkysiregar.feedzmore.ui.feature.home.feed.PostViewMode
import com.rizkysiregar.feedzmore.ui.feature.home.search.SearchViewModel
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.HomePathRoute

@Composable
fun ExploreRoute(
	viewModel: ExploreViewModel = hiltViewModel(),
	searchViewModel: SearchViewModel = hiltViewModel(),
	navController: NavController
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
	var selectedPostId by remember { mutableStateOf<String?>(null) }
	var commentPostId by remember { mutableStateOf<String?>(null) }
	val selectedPost = gridFeed.items.find { it.id == selectedPostId }

	val openDetail: (FeedContentItem) -> Unit = { post ->
		navController.currentBackStackEntry?.savedStateHandle?.set(
			KEY_POST_JSON,
			Gson().toJson(post)
		)
		navController.currentBackStackEntry?.savedStateHandle?.set(
			KEY_LIKE_HANDLER,
			viewModel::toggleLike
		)
		navController.navigate(HomePathRoute.PostDetail.create(post.id))
	}

	ExploreScreenContent(
		pagerState = pagerState,
		gridFeed = gridFeed,
		listFeed = listFeed,
		selectedPost = selectedPost,
		onGridPostClick = { post ->
			if (post.isTakenDown) return@ExploreScreenContent
			if (post.hasVideo) openDetail(post) else selectedPostId = post.id
		},
		onDismissDetail = { selectedPostId = null },
		onLikeClick = viewModel::toggleLike,
		onCommentClick = { commentPostId = it.id },
		onVideoClick = openDetail,
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
		onUserClick = { userId, username ->
			navController.navigate(HomePathRoute.UserDetail.create(userId, username))
		}
	)

	if (commentPostId != null) {
		CommentSectionSheet(
			postId = commentPostId!!,
			onDismiss = { commentPostId = null }
		)
	}
}
