package com.rizkysiregar.feedzmore.ui.feature.home.userdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 * Description: Software Engineer
 */

@Composable
fun UserDetailRoute(
	userId: String,
	username: String,
	onBackClick: () -> Unit,
	onFollowersClick: (String) -> Unit = {},
	onFollowingClick: (String) -> Unit = {},
	detailViewModel: UserDetailViewModel = hiltViewModel(),
	feedViewModel: UserDetailFeedViewModel = hiltViewModel()
) {
	val profileState by detailViewModel.profileState.collectAsState()
	val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()
	val followStatus by detailViewModel.followStatus.collectAsState()
	val isFollowActionLoading by detailViewModel.isFollowActionLoading.collectAsState()
	val listState = rememberLazyListState()

	LaunchedEffect(userId) {
		detailViewModel.loadUser(userId)
		detailViewModel.loadFollowStatus(userId)
	}

	LaunchedEffect(username) {
		feedViewModel.startFor(username)
	}

	// Load another page when the visible rows approach the end.
	LaunchedEffect(
		listState,
		feedState.hasMore,
		feedState.isLoadingMore,
		feedState.isRefreshing,
		feedState.isError
	) {
		snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
			.collectLatest { last ->
				val total = listState.layoutInfo.totalItemsCount
				if (last != null &&
					last >= total - 3 &&
					feedState.hasLoadedInitialPage &&
					!feedState.isLoadingMore &&
					feedState.hasMore &&
					!feedState.isRefreshing &&
					!feedState.isError
				) {
					feedViewModel.loadNextPage()
				}
			}
	}

	val state = profileState
	if (state == null) {
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			CircularProgressIndicator()
		}
	} else {
		UserDetailScreen(
			profileState = state,
			feedState = feedState,
			listState = listState,
			onBackClick = onBackClick,
			onRetry = { detailViewModel.loadUser(userId) },
			onRetryFeed = {
				if (feedState.hasLoadedInitialPage) feedViewModel.loadNextPage()
				else feedViewModel.refresh()
			},
			followStatus = followStatus,
			isFollowActionLoading = isFollowActionLoading,
			onFollowClick = { detailViewModel.toggleFollow(userId) },
			onFollowersClick = { onFollowersClick(userId) },
			onFollowingClick = { onFollowingClick(userId) }
		)
	}
}
