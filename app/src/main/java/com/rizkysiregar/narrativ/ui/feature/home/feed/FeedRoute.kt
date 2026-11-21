package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rizkysiregar.narrativ.ui.main.navigation.BottomNavScreen
import kotlinx.coroutines.flow.collectLatest

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 */

@Composable
fun FeedRoute(
    refreshVersion: Int = 0,
    viewModel: FeedViewModel = hiltViewModel(),
	onCreateFeed: () -> Unit,
	postOpener: PostOpener,
	onUserClick: (userId: String, username: String) -> Unit,
	onNotificationsClick: () -> Unit,
	unreadNotifications: Int,
	navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // This value changes only after a post is successfully uploaded.
    LaunchedEffect(refreshVersion) {
        if (refreshVersion > 0) {
            viewModel.refresh()
        }
    }

    // Load the next page near the end of the list, stopping at the API's last page.
    LaunchedEffect(
        listState,
        uiState.hasMore,
        uiState.isLoadingMore,
        uiState.isRefreshing,
        uiState.isError
    ) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collectLatest { last ->
                val total = listState.layoutInfo.totalItemsCount
                if (last != null &&
                    last >= total - 3 &&
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

    FeedScreen(
        uiState = uiState,
        onRefresh = { viewModel.refresh() },
        onRetry = {
            if (uiState.hasLoadedInitialPage) viewModel.loadNextPage() else viewModel.refresh()
        },
        listState = listState,
		onLikeClick = viewModel::toggleLike,
		onCreateFeed = onCreateFeed,
		onSearchClick = {
			navController.navigate(BottomNavScreen.Explore.route) {
				launchSingleTop = true
				restoreState = true
				popUpTo(navController.graph.startDestinationId) { saveState = true }
			}
		},
		player = viewModel.player,
		onAuthorBlocked = viewModel::refresh,
		onAuthorClick = onUserClick,
		unreadNotifications = unreadNotifications,
		onNotificationsClick = onNotificationsClick,
		onPostClick = { post -> postOpener.thread(post, viewModel::toggleLike) },
		onCommentClick = { post -> postOpener.thread(post, viewModel::toggleLike, openComments = true) },
		onMediaClick = { post, page -> postOpener.media(post, viewModel::toggleLike, page) }
    )
}
