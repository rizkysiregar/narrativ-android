package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.hilt.navigation.compose.hiltViewModel

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


	// Pagination trigger
	LaunchedEffect(listState) {
		snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
			.collect { lastVisible ->
				val total = listState.layoutInfo.totalItemsCount

				val shouldLoadMore = lastVisible != null &&
						lastVisible >= total - 3 &&
						!uiState.isLoading &&
						uiState.hasMore

				if (shouldLoadMore) {
					viewModel.loadNextPage()
				}

			}
	}


}