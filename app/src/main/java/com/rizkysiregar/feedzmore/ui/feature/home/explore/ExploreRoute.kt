package com.rizkysiregar.feedzmore.ui.feature.home.explore

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ExploreRoute(
	viewModel: ExploreViewModel = hiltViewModel()
) {
	val uiState by viewModel.uiState.collectAsState()
	val gridState = rememberLazyGridState()
	var selectedPostId by remember { mutableStateOf<String?>(null) }
	var commentPostId by remember { mutableStateOf<String?>(null) }
	val selectedPost = uiState.items.find { it.id == selectedPostId }

	LaunchedEffect(
		gridState,
		uiState.hasMore,
		uiState.isLoadingMore,
		uiState.isRefreshing,
		uiState.isError
	) {
		snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
			.collectLatest { last ->
				val total = gridState.layoutInfo.totalItemsCount
				if (last != null &&
					last >= total - 6 &&
					uiState.hasLoadedInitialPage &&
					!uiState.isLoadingMore &&
					uiState.hasMore &&
					!uiState.isRefreshing &&
					!uiState.isError
				) {
					viewModel.loadNextPage()
				}
			}
	}

	ExploreScreenContent(
		uiState = uiState,
		gridState = gridState,
		selectedPost = selectedPost,
		onPostClick = { selectedPostId = it.id },
		onDismissDetail = { selectedPostId = null },
		onLikeClick = viewModel::toggleLike,
		onCommentClick = { commentPostId = it.id }
	)

	if (commentPostId != null) {
		com.rizkysiregar.feedzmore.ui.feature.home.feed.CommentSectionSheet(
			postId = commentPostId!!,
			onDismiss = { commentPostId = null }
		)
	}
}
