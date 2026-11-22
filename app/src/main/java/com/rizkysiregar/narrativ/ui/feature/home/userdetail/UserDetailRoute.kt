package com.rizkysiregar.narrativ.ui.feature.home.userdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostOpener
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostViewMode

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 */

@Composable
fun UserDetailRoute(
	userId: String,
	username: String,
	onBackClick: () -> Unit,
	onFollowersClick: (String) -> Unit = {},
	onFollowingClick: (String) -> Unit = {},
	onOpenChat: (channelId: String) -> Unit = {},
	postOpener: PostOpener,
	openChatViewModel: com.rizkysiregar.narrativ.ui.feature.home.chat.OpenChatViewModel = hiltViewModel(),
	detailViewModel: UserDetailViewModel = hiltViewModel(),
	feedViewModel: UserDetailFeedViewModel = hiltViewModel()
) {
	val profileState by detailViewModel.profileState.collectAsState()
	val listFeed by feedViewModel.uiState.collectAsStateWithLifecycle()
	val gridFeed by feedViewModel.gridState.collectAsStateWithLifecycle()
	val followStatus by detailViewModel.followStatus.collectAsState()
	val isFollowActionLoading by detailViewModel.isFollowActionLoading.collectAsState()
	val currentUserId by detailViewModel.userId.collectAsState()
	val openChat by openChatViewModel.state.collectAsState()
	val context = androidx.compose.ui.platform.LocalContext.current
	val failedMessage = androidx.compose.ui.res.stringResource(com.rizkysiregar.narrativ.R.string.chat_open_failed)
	val notFoundMessage = androidx.compose.ui.res.stringResource(com.rizkysiregar.narrativ.R.string.chat_open_not_found)
	LaunchedEffect(openChat) {
		when (val result = openChat) {
			is com.rizkysiregar.narrativ.ui.feature.home.chat.OpenChatState.Opened -> { openChatViewModel.consumed(); onOpenChat(result.channelId) }
			is com.rizkysiregar.narrativ.ui.feature.home.chat.OpenChatState.Failed -> {
				android.widget.Toast.makeText(context, if (result.notFound) notFoundMessage else failedMessage, android.widget.Toast.LENGTH_LONG).show()
				openChatViewModel.consumed()
			}
			else -> Unit
		}
	}
	val pagerState = rememberPagerState(initialPage = PostViewMode.GRID.ordinal) { PostViewMode.entries.size }

	LaunchedEffect(userId) {
		detailViewModel.loadUser(userId)
		detailViewModel.loadFollowStatus(userId)
	}

	LaunchedEffect(username) {
		feedViewModel.startFor(username)
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
			pagerState = pagerState,
			gridFeed = gridFeed,
			listFeed = listFeed,
			onBackClick = onBackClick,
			onRetry = { detailViewModel.loadUser(userId) },
			onRetryGrid = {
				if (gridFeed.hasLoadedInitialPage) feedViewModel.loadNextGridPage()
				else feedViewModel.refreshGrid()
			},
			onRetryList = {
				if (listFeed.hasLoadedInitialPage) feedViewModel.loadNextPage()
				else feedViewModel.refresh()
			},
			onLoadMoreGrid = feedViewModel::loadNextGridPage,
			onLoadMoreList = feedViewModel::loadNextPage,
			onLikeClick = feedViewModel::toggleLike,
			followStatus = followStatus,
			onMessageClick = { openChatViewModel.open(userId) },
			isOpeningChat = openChat is com.rizkysiregar.narrativ.ui.feature.home.chat.OpenChatState.Opening,
			isFollowActionLoading = isFollowActionLoading,
			isOwnProfile = currentUserId.isNotBlank() && currentUserId == userId,
			onFollowClick = { detailViewModel.toggleFollow(userId) },
			onFollowersClick = { onFollowersClick(userId) },
			onFollowingClick = { onFollowingClick(userId) },
			onPostClick = { post -> postOpener.thread(post, feedViewModel::toggleLike) },
			onCommentClick = { post -> postOpener.thread(post, feedViewModel::toggleLike, openComments = true) },
			onMediaClick = { post, page -> postOpener.media(post, feedViewModel::toggleLike, page) },
			userId = userId,
			username = username,
			onUserBlocked = onBackClick
		)
	}
}
