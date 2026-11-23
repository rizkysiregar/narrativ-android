package com.rizkysiregar.narrativ.domain.feed

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.feed.SearchPostsRequest
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.narrativ.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchPostsUseCase @Inject constructor(
	private val repository: FeedRepository
) {
	operator fun invoke(request: SearchPostsRequest): Flow<ResultState<FeedsPostPaginationResponse>> {
		return repository.searchPosts(request)
	}
}
