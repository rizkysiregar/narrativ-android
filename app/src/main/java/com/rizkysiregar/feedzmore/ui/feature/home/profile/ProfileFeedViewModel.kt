package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetFeedByUsernameRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.feedzmore.domain.feed.DislikePostUseCase
import com.rizkysiregar.feedzmore.domain.feed.GetFeedByUsernameUseCase
import com.rizkysiregar.feedzmore.domain.feed.LikePostUseCase
import com.rizkysiregar.feedzmore.ui.feature.home.feed.BaseFeedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileFeedViewModel @Inject constructor(
    private val getFeedByUsernameUseCase: GetFeedByUsernameUseCase,
    dislikePostUseCase: DislikePostUseCase,
    likePostUseCase: LikePostUseCase,
    dataStore: DataStoreManager
) : BaseFeedViewModel(dataStore, dislikePostUseCase, likePostUseCase) {

    init {
        viewModelScope.launch {
            // suspend until a real username is available from datastore
            val username = usernameDataStore.first { it.isNotBlank() }
            refresh()
        }
    }

    override suspend fun fetchPage(
        page: Int,
        size: Int
    ): Flow<ResultState<FeedsPostPaginationResponse>> {
        val username = usernameDataStore.value
        return getFeedByUsernameUseCase(
            GetFeedByUsernameRequest(
                page = page,
                size = size,
                username = username
            )
        )
    }


}