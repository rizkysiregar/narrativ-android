package com.rizkysiregar.feedzmore.ui.feature.home.feed

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.feedzmore.domain.feed.DislikePostUseCase
import com.rizkysiregar.feedzmore.domain.feed.LikePostUseCase
import com.rizkysiregar.feedzmore.ui.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

abstract class BaseFeedViewModel(
    dataStore: DataStoreManager,
    private val dislikePostUseCase: DislikePostUseCase,
    private val likePostUseCase: LikePostUseCase,
) : BaseViewModel(dataStore) {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var loadGeneration = 0

    // Posts with a like/dislike request in flight; extra taps are ignored until it settles.
    private val likeInFlight = mutableSetOf<String>()

    abstract suspend fun fetchPage(
        page: Int,
        size: Int
    ): Flow<ResultState<FeedsPostPaginationResponse>>

    fun refresh() {
        loadGeneration++
        loadJob?.cancel()
        val generation = loadGeneration
        _uiState.value = FeedUiState(isRefreshing = true)
        loadJob = viewModelScope.launch {
            loadPage(page = 0, size = _uiState.value.pageSize, generation = generation)
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (!state.hasLoadedInitialPage || !state.hasMore ||
            state.isLoadingMore || state.isRefreshing
        ) return

        _uiState.update { it.copy(isLoadingMore = true, errorMessage = null, isError = false) }
        val generation = loadGeneration
        loadJob = viewModelScope.launch {
            loadPage(page = state.page, size = state.pageSize, generation = generation)
        }
    }

    private suspend fun loadPage(page: Int, size: Int, generation: Int) {
        fetchPage(page, size).collect { result ->
            if (generation != loadGeneration) return@collect
            when (result) {
                is ResultState.Loading -> Unit
                is ResultState.Success -> {
                    val response = result.data
                    _uiState.update {
                        it.copy(
                            items = if (page == 0) response.content else it.items + response.content,
                            page = page + 1,
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
                    _uiState.update {
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

    fun toggleLike(postId: String) {
        if (postId in likeInFlight) return
        val original = _uiState.value.items.firstOrNull { it.id == postId } ?: return
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
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.id == postId) transform(it) else it })
        }
    }

    private companion object {
        const val TAG = "BaseFeedViewModel"
    }
}
