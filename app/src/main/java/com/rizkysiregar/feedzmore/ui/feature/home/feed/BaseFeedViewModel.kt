package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
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
        val currentList = _uiState.value.items
        val itemIndex = currentList.indexOfFirst { it.id == postId }
        if (itemIndex == -1) return

        val currentItem = currentList[itemIndex]
        val isCurrentlyLiked = currentItem.liked

        _uiState.value = _uiState.value.copy(
            items = currentList.toMutableList().apply {
                set(
                    itemIndex, currentItem.copy(
                        liked = !isCurrentlyLiked,
                        likeCount = if (isCurrentlyLiked) currentItem.likeCount - 1
                        else currentItem.likeCount + 1
                    )
                )
            }
        )

        viewModelScope.launch {
            val response = if (isCurrentlyLiked) dislikePostUseCase(UUID.fromString(postId))
            else likePostUseCase(UUID.fromString(postId))

            response.collect { result ->
                when (result) {
                    is ResultState.Success -> println("toggle like success")
                    is ResultState.Failure -> println("## toggle like failure: ${result.message}")
                    else -> {}
                }
            }
        }
    }
}
