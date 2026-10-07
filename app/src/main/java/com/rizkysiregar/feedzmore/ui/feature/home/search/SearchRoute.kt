package com.rizkysiregar.feedzmore.ui.feature.home.search

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 * Description: Software Engineer
 */

@Composable
fun SearchRoute(
	onUserClick: (userId: String, username: String) -> Unit,
	viewModel: SearchViewModel = hiltViewModel()
) {
	val uiState by viewModel.uiState.collectAsState()
	val listState = rememberLazyListState()

	LaunchedEffect(uiState.query) {
		listState.scrollToItem(0)
	}

	LaunchedEffect(
		listState,
		uiState.hasMore,
		uiState.isLoadingMore,
		uiState.isLoading,
		uiState.isError,
		uiState.query
	) {
		snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
			.collectLatest { last ->
				val total = listState.layoutInfo.totalItemsCount
				if (last != null &&
					last >= total - 3 &&
					uiState.hasLoadedInitialPage &&
					!uiState.isLoadingMore &&
					uiState.hasMore &&
					!uiState.isLoading &&
					!uiState.isError
				) {
					viewModel.loadNextPage()
				}
			}
	}

	SearchScreen(
		uiState = uiState,
		onQueryChange = viewModel::onQueryChange,
		onRetry = viewModel::retry,
		onUserClick = onUserClick,
		listState = listState
	)
}
