package com.rizkysiregar.feedzmore.domain.feed

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetFeedPostRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.feedzmore.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */
class GetFeedPostUseCase @Inject constructor(
	private val repository: FeedRepository
) {
	operator fun invoke(request: GetFeedPostRequest): Flow<ResultState<FeedsPostPaginationResponse>> {
		return repository.getFeed(request)
	}
}