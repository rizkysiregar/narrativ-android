package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetAllFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetFeedByUsernameRequest
import com.rizkysiregar.feedzmore.domain.feed.DislikePostUseCase
import com.rizkysiregar.feedzmore.domain.feed.GetAllFeedUseCase
import com.rizkysiregar.feedzmore.domain.feed.GetFeedByUsernameUseCase
import com.rizkysiregar.feedzmore.domain.feed.LikePostUseCase
import com.rizkysiregar.feedzmore.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.UUID
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getAllFeedUseCase: GetAllFeedUseCase,
    private val getFeedByUsernameUseCase: GetFeedByUsernameUseCase,
    private val dislikePostUseCase: DislikePostUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val dataStore: DataStoreManager,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel(dataStore) {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val loadMutex = Mutex()
    private val endlessLoopEnabled = true // toggle: true = loop when last == true

    private var username: String? = savedStateHandle["username"]

    init {
        // initial load
        println("## FeedViewModel init")
        viewModelScope.launch {
            refresh()
        }
    }

    fun setUsername(name: String) {
        if (username == null) {
            username = name
        }
    }

    // Public: pull-to-refresh
    fun refresh() {
        println("## viewModel.refresh() called")
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

    // Public: load next page (scroll)
    fun loadNextPage() {
        println("## viewModel.loadNextPage() called")
        _uiState.update { it.copy(isError = false) }
        val state = _uiState.value
        if (!state.hasMore || state.isLoadingMore || state.isRefreshing) return

        viewModelScope.launch {
            if (!loadMutex.tryLock()) return@launch
            try {
                _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
                loadPage(
                    page = state.page,
                    size = state.pageSize,
                )
            } finally {
                loadMutex.unlock()
            }
        }
    }

    // Internal loader — collects usecase flow safely (no nested collectors)
    private suspend fun loadPage(page: Int, size: Int) {
        println("## viewModel.loadPage() called")

        val flow = if (username != null) {
            getFeedByUsernameUseCase(
                GetFeedByUsernameRequest(page, size, username!!)
            )
        } else {
            getAllFeedUseCase(
                GetAllFeedRequest(page, size)
            )
        }

        flow.collect { result ->
            when (result) {
                is ResultState.Loading -> {
                    // optional: keep states already set (isRefreshing/isLoadingMore)
                }

                is ResultState.Success -> {
                    val resp = result.data

                    if (resp.last && endlessLoopEnabled) {
                        // If last and we want to loop: fetch page 0 and append, without blocking UI
                        // We perform a single request for page 0 and append result
                        // Use first() to get single emission (Loading->Success/Failure)
                        val restart = getAllFeedUseCase(
                            GetAllFeedRequest(
                                page = 0,
                                size = size
                            )
                        ).firstOrNull()
                        when (restart) {
                            is ResultState.Success -> {
                                _uiState.update {
                                    it.copy(
                                        items = it.items + resp.content + restart.data.content, // append last page then loop start
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
                        // Normal append
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
                    println("## viewModel.loadPage() Failure: ${result.message}")
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


    // toggle like on post
    fun toggleLike(postId: String) {
        println("## ViewModel Received Like Click for ID: $postId")

        // 1. find item on the list
        val currentList = _uiState.value.items
        val itemIndex = currentList.indexOfFirst { it.id == postId }
        if (itemIndex == -1) return


        val currentItem = currentList[itemIndex]
        val isCurrentlyLiked = currentItem.liked

        // 2. Update the the ui state immediately
        val updatedItem = currentItem.copy(
            liked = !isCurrentlyLiked,
            likeCount = if (isCurrentlyLiked) currentItem.likeCount - 1 else currentItem.likeCount + 1
        )

        // Update the stateflow list with the updated item
        _uiState.value = _uiState.value.copy(
            items = currentList.toMutableList().apply {
                set(itemIndex, updatedItem)
            }
        )

        // 3. send to API
        viewModelScope.launch {
            val response = if (isCurrentlyLiked) { // dislike
                dislikePostUseCase(UUID.fromString(postId))
            } else { // like
                likePostUseCase(UUID.fromString(postId))
            }

            response.collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        println("toggle like success")
                    }

                    is ResultState.Failure -> {
                        println("## toggle like failure: ${result.message}")
                    }

                    else -> {}
                }
            }

        }
    }
}