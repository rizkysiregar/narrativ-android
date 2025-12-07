package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@Composable
fun FeedRoute(
	viewModel: FeedViewModel = hiltViewModel()
) {
	val uiState by viewModel.uiState.collectAsState()
	val listState = rememberLazyListState()
	val userId by viewModel.userId.collectAsState()
	val token by viewModel.token.collectAsState()

	// Detect near-bottom and trigger loadNextPage
	LaunchedEffect(listState, uiState.hasMore, uiState.isLoadingMore, uiState.isRefreshing) {
		snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
			.collectLatest { last ->
				val total = listState.layoutInfo.totalItemsCount
				if (last != null && last >= total - 3 && !uiState.isLoadingMore && uiState.hasMore && !uiState.isRefreshing) {
					viewModel.loadNextPage(userId)
				}
			}
	}

	if (token.isNotEmpty()) {
		FeedScreen(
			uiState = uiState,
			onRefresh = { viewModel.refresh(userId) },
			onRetry = { viewModel.loadNextPage(userId) },
			listState = listState,
			token = token
		)
	} else {
		CircularProgressIndicator()
	}

}