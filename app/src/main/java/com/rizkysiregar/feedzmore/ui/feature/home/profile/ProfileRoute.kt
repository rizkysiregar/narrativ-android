package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedViewModel
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@Composable
fun ProfileRoute(
    profileVm: ProfileViewModel = hiltViewModel(),
    profileFeedVm: ProfileFeedViewModel = hiltViewModel(),
    onLogoutClick: () -> Unit
) {
    val profileState by profileVm.profileState.collectAsState()
    val feedState by profileFeedVm.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // only fetch profile info, ProfileFeedViewModel handles its own init load
    LaunchedEffect(Unit) {
        profileVm.fetchUserProfile()
    }

    // infinite scroll handler — same pattern as FeedRoute
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
                    !feedState.isLoadingMore &&
                    feedState.hasMore &&
                    !feedState.isRefreshing &&
                    !feedState.isError
                ) {
                    profileFeedVm.loadNextPage()
                }
            }
    }



    profileState?.let {
        ProfileScreen(
            profileState = it,
            feedState = feedState,
            listState = listState,
            onRetry = { profileVm.fetchUserProfile() },
            onRefresh = { profileFeedVm.refresh() },
            onLoadNextPage = { profileFeedVm.loadNextPage() },
            onLikeClick = profileFeedVm::toggleLike,
            onExitClick = {
                profileVm.clearDataStore()
                onLogoutClick()
            }
        )
    }


}


@Preview(showBackground = true)
@Composable
private fun ProfileRoutePreview() {
    FeedzmoreTheme {
        ProfileRoute(
            onLogoutClick = {}
        )
    }
}