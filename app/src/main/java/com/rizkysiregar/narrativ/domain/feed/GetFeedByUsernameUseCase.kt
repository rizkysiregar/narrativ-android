package com.rizkysiregar.narrativ.domain.feed

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.feed.GetFeedByUsernameRequest
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.narrativ.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFeedByUsernameUseCase @Inject constructor(
    private val repository: FeedRepository
) {
    operator fun invoke(request: GetFeedByUsernameRequest): Flow<ResultState<FeedsPostPaginationResponse>> {
        return repository.getFeedByUsername(request)
    }

}