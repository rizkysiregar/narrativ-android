package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
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
    refreshVersion: Int = 0,
    viewModel: FeedViewModel = hiltViewModel(),
	onCreateFeed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // This value changes only after a post is successfully uploaded.
    LaunchedEffect(refreshVersion) {
        if (refreshVersion > 0) {
            viewModel.refresh()
        }
    }

    /**
     * INFINITE SCROLL / PAGINATION HANDLER
     *
     * Purpose: Automatically fetches the next page of data when the user scrolls near the bottom.
     *
     * Logic:
     * 1. Monitors [listState] to detect the current scroll position.
     * 2. Triggers [viewModel.loadNextPage] when the user is 3 items away from the end.
     * 3. Prevents "Request Storming" (infinite loops) by validating:
     *    - Not currently loading.
     *    - More data actually exists on the server.
     *    - Not currently refreshing the entire list.
     *    - No active error state (stops retrying if the backend is down/530 etc).
     *
     * Note: If [uiState.isError] is true, automatic fetching halts to save battery/bandwidth.
     * User must manually trigger a retry to reset the [isError] flag.
     */
    LaunchedEffect(
        listState,
        uiState.hasMore,
        uiState.isLoadingMore,
        uiState.isRefreshing,
        uiState.isError
    ) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collectLatest { last ->
                println("collect latest called")
                val total = listState.layoutInfo.totalItemsCount
                if (last != null &&
                    last >= total - 3 &&
                    !uiState.isLoadingMore &&
                    uiState.hasMore &&
                    !uiState.isRefreshing &&
                    !uiState.isError // <-- stop if there is an error
                ) {
                    println("## last: $last, total: $total")
                    viewModel.loadNextPage()
                }
            }
    }

    FeedScreen(
        uiState = uiState,
        onRefresh = { viewModel.refresh() },
        onRetry = { viewModel.loadNextPage() },
        listState = listState,
		onLikeClick = viewModel::toggleLike,
		onCreateFeed = onCreateFeed
    )
}
