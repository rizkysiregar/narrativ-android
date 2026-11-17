package com.rizkysiregar.narrativ.domain.feed

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.feed.GetAllFeedRequest
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.narrativ.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExplorePostsUseCase @Inject constructor(
	private val repository: FeedRepository
) {
	operator fun invoke(request: GetAllFeedRequest): Flow<ResultState<FeedsPostPaginationResponse>> {
		return repository.getExplorePosts(request)
	}
}
