package com.rizkysiregar.feedzmore.domain.feed

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.PostingFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.PostingFeedResponse
import com.rizkysiregar.feedzmore.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 09/12/25
 * Description: Software Engineer
 */
class PostingFeedUseCase @Inject constructor(
	private val repository: FeedRepository
) {
	operator fun invoke(postingFeedRequest: PostingFeedRequest): Flow<ResultState<PostingFeedResponse>> {
		return repository.postingFeed(postingFeedRequest)
	}
}