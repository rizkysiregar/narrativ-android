package com.rizkysiregar.narrativ.ui.feature.home.profile

import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.feed.GetFeedByUsernameRequest
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.narrativ.domain.feed.DislikePostUseCase
import com.rizkysiregar.narrativ.domain.feed.GetFeedByUsernameUseCase
import com.rizkysiregar.narrativ.domain.feed.LikePostUseCase
import com.rizkysiregar.narrativ.ui.feature.home.feed.BaseFeedViewModel
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
            usernameDataStore.first { it.isNotBlank() }
            refreshAll()
        }
    }

    override suspend fun fetchPage(
        page: Int,
        size: Int,
        cursor: String?,
        mediaOnly: Boolean
    ): Flow<ResultState<FeedsPostPaginationResponse>> {
        val username = usernameDataStore.value
        return getFeedByUsernameUseCase(
            GetFeedByUsernameRequest(
                page = page,
                size = size,
                username = username,
                mediaOnly = mediaOnly,
                cursor = cursor
            )
        )
    }


}