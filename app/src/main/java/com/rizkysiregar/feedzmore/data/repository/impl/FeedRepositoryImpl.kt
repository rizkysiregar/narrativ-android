package com.rizkysiregar.feedzmore.data.repository.impl

import com.rizkysiregar.feedzmore.data.remote.api.FeedApi
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.GetFeedPostRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.PostingFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.FeedsPostResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.PostingFeedResponse
import com.rizkysiregar.feedzmore.data.repository.FeedRepository
import com.rizkysiregar.feedzmore.util.flowFromApi
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */
class FeedRepositoryImpl @Inject constructor(
	private val retrofit: Retrofit
) : FeedRepository {
	private val feedApi: FeedApi by lazy {
		retrofit.create(FeedApi::class.java)
	}

	override fun getFeed(feedRequest: GetFeedPostRequest): Flow<ResultState<FeedsPostResponse>> =
		flowFromApi {
			feedApi.getFeed(feedRequest.page, feedRequest.size, feedRequest.userId)

		}

	override fun postingFeed(postingFeedRequest: PostingFeedRequest): Flow<ResultState<PostingFeedResponse>> =
		flowFromApi {
			feedApi.postingFeed(
				addPostRequest = postingFeedRequest,
				image = postingFeedRequest.imagePart!!
			)
		}


}