package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.lifecycle.SavedStateHandle
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetAllFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.feedzmore.domain.feed.DislikePostUseCase
import com.rizkysiregar.feedzmore.domain.feed.GetAllFeedUseCase
import com.rizkysiregar.feedzmore.domain.feed.LikePostUseCase
import com.rizkysiregar.feedzmore.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
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
        refresh()
    }
    override suspend fun fetchPage(
        page: Int,
        size: Int
    ): Flow<ResultState<FeedsPostPaginationResponse>> =
        getAllFeedUseCase(GetAllFeedRequest(page, size))

}
