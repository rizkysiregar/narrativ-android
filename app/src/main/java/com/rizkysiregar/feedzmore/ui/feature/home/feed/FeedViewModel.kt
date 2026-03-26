package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetAllFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetFeedByUsernameRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.feedzmore.domain.feed.DislikePostUseCase
import com.rizkysiregar.feedzmore.domain.feed.GetAllFeedUseCase
import com.rizkysiregar.feedzmore.domain.feed.GetFeedByUsernameUseCase
import com.rizkysiregar.feedzmore.domain.feed.LikePostUseCase
import com.rizkysiregar.feedzmore.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
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
    dislikePostUseCase: DislikePostUseCase,
    likePostUseCase: LikePostUseCase,
    dataStore: DataStoreManager
) : BaseFeedViewModel(dataStore, dislikePostUseCase, likePostUseCase) {

    init {
        viewModelScope.launch {
            refresh()
        }
    }
    override suspend fun fetchPage(
        page: Int,
        size: Int
    ): Flow<ResultState<FeedsPostPaginationResponse>> =
        getAllFeedUseCase(GetAllFeedRequest(page, size))

    // loop back to page 0 using getAllFeed (same use case)
    override suspend fun fetchLoopPage(size: Int): Flow<ResultState<FeedsPostPaginationResponse>> =
        getAllFeedUseCase(GetAllFeedRequest(page = 0, size = size))

}