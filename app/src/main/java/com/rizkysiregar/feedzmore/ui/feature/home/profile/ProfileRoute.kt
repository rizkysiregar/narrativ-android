package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.ui.feature.home.feed.PostViewMode
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@Composable
fun ProfileRoute(
    profileVm: ProfileViewModel = hiltViewModel(),
    profileFeedVm: ProfileFeedViewModel = hiltViewModel(),
	onSettingsClick: () -> Unit,
	onFollowersClick: (String) -> Unit = {},
	onFollowingClick: (String) -> Unit = {},
	onPostClick: (FeedContentItem, (String) -> Unit) -> Unit = { _, _ -> }
) {
    val profileState by profileVm.profileState.collectAsState()
    val followStatus by profileVm.followStatus.collectAsState()
    val listFeed by profileFeedVm.uiState.collectAsStateWithLifecycle()
    val gridFeed by profileFeedVm.gridState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = PostViewMode.GRID.ordinal) { PostViewMode.entries.size }

    // only fetch profile info, ProfileFeedViewModel handles its own init load
    LaunchedEffect(Unit) {
        profileVm.fetchUserProfile()
    }

    profileState?.let {
        ProfileScreen(
            profileState = it,
            pagerState = pagerState,
            gridFeed = gridFeed,
            listFeed = listFeed,
            onRetry = { profileVm.fetchUserProfile() },
            onRetryGrid = {
                if (gridFeed.hasLoadedInitialPage) profileFeedVm.loadNextGridPage()
                else profileFeedVm.refreshGrid()
            },
            onRetryList = {
                if (listFeed.hasLoadedInitialPage) profileFeedVm.loadNextPage()
                else profileFeedVm.refresh()
            },
            onLoadMoreGrid = profileFeedVm::loadNextGridPage,
            onLoadMoreList = profileFeedVm::loadNextPage,
            onRefresh = {
                profileVm.fetchUserProfile()
                profileFeedVm.refreshAll()
            },
            onLikeClick = profileFeedVm::toggleLike,
            onSettingsClick = onSettingsClick,
            followStatus = followStatus,
            onFollowersClick = { userId -> onFollowersClick(userId) },
            onFollowingClick = { userId -> onFollowingClick(userId) },
            onPostClick = { post -> onPostClick(post, profileFeedVm::toggleLike) }
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun ProfileRoutePreview() {
    FeedzmoreTheme {
        ProfileRoute(
            onSettingsClick = {}
        )
    }
}
