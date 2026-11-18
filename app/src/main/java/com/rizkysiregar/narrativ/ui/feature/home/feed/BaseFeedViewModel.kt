package com.rizkysiregar.narrativ.ui.feature.home.feed

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.narrativ.domain.feed.DislikePostUseCase
import com.rizkysiregar.narrativ.domain.feed.LikePostUseCase
import com.rizkysiregar.narrativ.ui.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Pages posts in two independent streams: [uiState] holds every post (the list view and the home
 * feed), [gridState] only posts holding media (the grid view). The grid stays empty until
 * [refreshGrid] is called, and a like toggled in one stream shows up in the other too.
 */
abstract class BaseFeedViewModel(
    dataStore: DataStoreManager,
    private val dislikePostUseCase: DislikePostUseCase,
    private val likePostUseCase: LikePostUseCase,
) : BaseViewModel(dataStore) {

    private val listPaging = FeedPaging(mediaOnly = false)
    private val gridPaging = FeedPaging(mediaOnly = true)

    val uiState: StateFlow<FeedUiState> = listPaging.state
    val gridState: StateFlow<FeedUiState> = gridPaging.state

    // Posts with a like/dislike request in flight; extra taps are ignored until it settles.
    private val likeInFlight = mutableSetOf<String>()

    /**
     * One page of the feed. [cursor] is the nextCursor of the previous page (null for the first one); with it the
     * server returns the posts after that one, so new posts arriving while scrolling never repeat a post on the next
     * page. [page] is what a server without cursors goes by.
     */
    abstract suspend fun fetchPage(
        page: Int,
        size: Int,
        cursor: String?,
        mediaOnly: Boolean
    ): Flow<ResultState<FeedsPostPaginationResponse>>

    fun refresh() = listPaging.refresh()

    fun loadNextPage() = listPaging.loadNextPage()

    fun refreshGrid() = gridPaging.refresh()

    fun loadNextGridPage() = gridPaging.loadNextPage()

    fun refreshAll() {
        refresh()
        refreshGrid()
    }

    fun toggleLike(postId: String) {
        if (postId in likeInFlight) return
        val original = uiState.value.items.firstOrNull { it.id == postId }
            ?: gridState.value.items.firstOrNull { it.id == postId }
            ?: return
        val wasLiked = original.liked

        likeInFlight += postId
        updateItem(postId) {
            it.copy(
                liked = !wasLiked,
                likeCount = if (wasLiked) it.likeCount - 1 else it.likeCount + 1
            )
        }

        viewModelScope.launch {
            val response = if (wasLiked) dislikePostUseCase(UUID.fromString(postId))
            else likePostUseCase(UUID.fromString(postId))

            try {
                response.collect { result ->
                    if (result is ResultState.Failure) {
                        Log.w(TAG, "toggle like failed: ${result.message}")
                        // Server rejected it: restore what the user saw before tapping.
                        updateItem(postId) {
                            it.copy(liked = original.liked, likeCount = original.likeCount)
                        }
                    }
                }
            } finally {
                likeInFlight -= postId
            }
        }
    }

    private fun updateItem(postId: String, transform: (FeedContentItem) -> FeedContentItem) {
        listPaging.updateItem(postId, transform)
        gridPaging.updateItem(postId, transform)
    }

    private inner class FeedPaging(private val mediaOnly: Boolean) {
        private val _state = MutableStateFlow(FeedUiState())
        val state: StateFlow<FeedUiState> = _state.asStateFlow()

        private var loadJob: Job? = null
        private var loadGeneration = 0

        fun refresh() {
            loadGeneration++
            loadJob?.cancel()
            val generation = loadGeneration
            _state.value = FeedUiState(isRefreshing = true)
            loadJob = viewModelScope.launch {
                loadPage(page = 0, cursor = null, size = _state.value.pageSize, generation = generation)
            }
        }

        fun loadNextPage() {
            val state = _state.value
            if (!state.hasLoadedInitialPage || !state.hasMore ||
                state.isLoadingMore || state.isRefreshing
            ) return

            _state.update { it.copy(isLoadingMore = true, errorMessage = null, isError = false) }
            val generation = loadGeneration
            loadJob = viewModelScope.launch {
                loadPage(page = state.page, cursor = state.nextCursor, size = state.pageSize, generation = generation)
            }
        }

        fun updateItem(postId: String, transform: (FeedContentItem) -> FeedContentItem) {
            _state.update { state ->
                state.copy(items = state.items.map { if (it.id == postId) transform(it) else it })
            }
        }

        private suspend fun loadPage(page: Int, cursor: String?, size: Int, generation: Int) {
            fetchPage(page, size, cursor, mediaOnly).collect { result ->
                if (generation != loadGeneration) return@collect
                when (result) {
                    is ResultState.Loading -> Unit
                    is ResultState.Success -> {
                        val response = result.data
                        _state.update {
                            it.copy(
                                items = if (page == 0) response.content else it.items + response.content,
                                page = page + 1,
                                nextCursor = response.nextCursor,
                                hasLoadedInitialPage = true,
                                isLoadingMore = false,
                                isRefreshing = false,
                                hasMore = !response.last && response.content.isNotEmpty(),
                                errorMessage = null,
                                isError = false
                            )
                        }
                    }
                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                isLoadingMore = false,
                                isRefreshing = false,
                                errorMessage = result.message,
                                isError = true
                            )
                        }
                    }
                    is ResultState.Idle -> Unit
                }
            }
        }
    }

    private companion object {
        const val TAG = "BaseFeedViewModel"
    }
}
