package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rizkysiregar.narrativ.ui.feature.home.search.HomeSearchContent
import com.rizkysiregar.narrativ.ui.feature.home.search.SearchPostsViewModel
import com.rizkysiregar.narrativ.ui.feature.home.search.SearchTab
import com.rizkysiregar.narrativ.ui.feature.home.search.SearchViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 */

@Composable
fun FeedRoute(
    refreshVersion: Int = 0,
    viewModel: FeedViewModel = hiltViewModel(),
	searchViewModel: SearchViewModel = hiltViewModel(),
	searchPostsViewModel: SearchPostsViewModel = hiltViewModel(),
	onCreateFeed: () -> Unit,
	postOpener: PostOpener,
	onUserClick: (userId: String, username: String) -> Unit,
	onNotificationsClick: () -> Unit,
	unreadNotifications: Int,
	unreadMessages: Int = 0,
	onMessagesClick: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // the search lives in home: the search button opens it in place of the feed, back closes it
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var searchTab by rememberSaveable { mutableStateOf(SearchTab.TOP) }
    val focusManager = LocalFocusManager.current
    val onQueryChange = { query: String ->
        searchViewModel.onQueryChange(query)
        searchPostsViewModel.onQueryChange(query)
    }
    val closeSearch = {
        onQueryChange("")
        focusManager.clearFocus()
        searchTab = SearchTab.TOP
        isSearchOpen = false
    }
    BackHandler(enabled = isSearchOpen, onBack = closeSearch)
    if (isSearchOpen) {
        val people by searchViewModel.uiState.collectAsState()
        val posts by searchPostsViewModel.uiState.collectAsState()
        HomeSearchContent(
            people = people,
            posts = posts,
            tab = searchTab,
            onTabChange = { searchTab = it },
            onQueryChange = onQueryChange,
            onClose = closeSearch,
            onRetryPeople = searchViewModel::retry,
            onLoadMorePeople = searchViewModel::loadNextPage,
            onRetryPosts = {
                if (posts.hasLoadedInitialPage) searchPostsViewModel.loadNextPage() else searchPostsViewModel.refresh()
            },
            onLoadMorePosts = searchPostsViewModel::loadNextPage,
            onUserClick = onUserClick,
            onLikeClick = searchPostsViewModel::toggleLike,
            onPostClick = { post -> postOpener.thread(post, searchPostsViewModel::toggleLike) },
            onCommentClick = { post -> postOpener.thread(post, searchPostsViewModel::toggleLike, openComments = true) },
            onMediaClick = { post, page -> postOpener.media(post, searchPostsViewModel::toggleLike, page) },
            onAuthorBlocked = {
                searchPostsViewModel.refresh()
                searchViewModel.retry()
                viewModel.refresh()
            }
        )
        return
    }

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
		onSearchClick = { isSearchOpen = true },
		player = viewModel.player,
		onAuthorBlocked = viewModel::refresh,
		onAuthorClick = onUserClick,
		unreadNotifications = unreadNotifications,
		onNotificationsClick = onNotificationsClick,
		unreadMessages = unreadMessages,
		onMessagesClick = onMessagesClick,
		onPostClick = { post -> postOpener.thread(post, viewModel::toggleLike) },
		onCommentClick = { post -> postOpener.thread(post, viewModel::toggleLike, openComments = true) },
		onMediaClick = { post, page -> postOpener.media(post, viewModel::toggleLike, page) }
    )
}
