package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.feedzmore.domain.feed.DislikePostUseCase
import com.rizkysiregar.feedzmore.domain.feed.LikePostUseCase
import com.rizkysiregar.feedzmore.ui.base.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

abstract class BaseFeedViewModel(
    dataStore: DataStoreManager,
    private val dislikePostUseCase: DislikePostUseCase,
    private val likePostUseCase: LikePostUseCase,
) : BaseViewModel(dataStore) {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val loadMutex = Mutex()
    private val endlessLoopEnabled = true

    // each subclass provides the correct data source
    abstract suspend fun fetchPage(
        page: Int,
        size: Int
    ): Flow<ResultState<FeedsPostPaginationResponse>>

    // override this in FeedViewModel to loop back to page 0 using getAllFeed
    // default just fetches page 0 using the same fetchPage (used by profile)
    open suspend fun fetchLoopPage(size: Int): Flow<ResultState<FeedsPostPaginationResponse>> =
        fetchPage(0, size)


    fun refresh() {
        println("## BaseFeedViewModel.refresh() called")
        _uiState.update { it.copy(isError = false) }
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    errorMessage = null,
                    items = emptyList(),
                    page = 0,
                    pageSize = 10,
                    hasMore = true
                )
            }
            loadPage(page = 0, size = _uiState.value.pageSize)
        }
    }

    fun loadNextPage() {
        println("## BaseFeedViewModel.loadNextPage() called")
        _uiState.update { it.copy(isError = false) }
        val state = _uiState.value
        if (!state.hasMore || state.isLoadingMore || state.isRefreshing) return

        viewModelScope.launch {
            if (!loadMutex.tryLock()) return@launch
            try {
                _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
                loadPage(page = state.page, size = state.pageSize)
            } finally {
                loadMutex.unlock()
            }
        }
    }


    private suspend fun loadPage(page: Int, size: Int) {
        println("## BaseFeedViewModel.loadPage() called, page: $page")
        fetchPage(page, size).collect { result ->
            when (result) {
                is ResultState.Loading -> {}

                is ResultState.Success -> {
                    val resp = result.data

                    if (resp.last && endlessLoopEnabled) {
                        val restart = fetchLoopPage(size).firstOrNull()
                        when (restart) {
                            is ResultState.Success -> {
                                _uiState.update {
                                    it.copy(
                                        items = it.items + resp.content + restart.data.content,
                                        page = 1,
                                        isLoadingMore = false,
                                        isRefreshing = false,
                                        hasMore = true
                                    )
                                }
                            }

                            is ResultState.Failure -> {
                                _uiState.update {
                                    it.copy(
                                        items = it.items + resp.content,
                                        page = page + 1,
                                        isLoadingMore = false,
                                        isRefreshing = false,
                                        hasMore = false,
                                        errorMessage = restart.message
                                    )
                                }
                            }

                            else -> {
                                _uiState.update {
                                    it.copy(
                                        items = it.items + resp.content,
                                        page = page + 1,
                                        isLoadingMore = false,
                                        isRefreshing = false,
                                        hasMore = !resp.last
                                    )
                                }
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                items = it.items + resp.content,
                                page = page + 1,
                                isLoadingMore = false,
                                isRefreshing = false,
                                hasMore = !resp.last
                            )
                        }
                    }
                }

                is ResultState.Failure -> {
                    println("## BaseFeedViewModel.loadPage() Failure: ${result.message}")
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
        println("## BaseFeedViewModel toggleLike for ID: $postId")
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