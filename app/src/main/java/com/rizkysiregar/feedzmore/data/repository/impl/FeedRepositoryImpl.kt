package com.rizkysiregar.feedzmore.data.repository.impl

import com.rizkysiregar.feedzmore.data.remote.api.FeedApi
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.GetFeedPostRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.PostingFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.FeedsPostResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.PostingFeedResponse
import com.rizkysiregar.feedzmore.data.repository.FeedRepository
import com.rizkysiregar.feedzmore.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
		flow {
			emit(ResultState.Loading)
			val result = safeApiCall {
				feedApi.getFeed(feedRequest.page, feedRequest.size, feedRequest.userId)
			}

			when (result) {
				is ResultState.Success -> {
					val data = result.data

					if (data.data != null) {
						emit(ResultState.Success(data.data))
					} else {
						emit(ResultState.Failure(data.message))
					}
				}

				is ResultState.Failure -> emit(result)
				else -> {}
			}
		}

	override fun postingFeed(postingFeedRequest: PostingFeedRequest): Flow<ResultState<PostingFeedResponse>> =
		flow {
			emit(ResultState.Loading)

			val result = safeApiCall {
				feedApi.postingFeed(
					addPostRequest = postingFeedRequest,
					image = postingFeedRequest.imagePart!!
				)
			}

			when (result) {
				is ResultState.Success -> {
					val data = result.data

					if (data.data != null) {
						emit(ResultState.Success(data = data.data))
					} else {
						emit(ResultState.Failure(data.message))
					}
				}

				is ResultState.Failure -> {
					emit(result)
				}

				else -> {}
			}
		}


}