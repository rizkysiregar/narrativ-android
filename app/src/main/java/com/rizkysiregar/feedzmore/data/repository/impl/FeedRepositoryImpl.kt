package com.rizkysiregar.feedzmore.data.repository.impl

import com.rizkysiregar.feedzmore.data.remote.api.FeedApi
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.AddCommentRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetCommentRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetAllFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetFeedByUsernameRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.LikeFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.feedzmore.data.repository.FeedRepository
import com.rizkysiregar.feedzmore.util.flowFromApi
import com.rizkysiregar.feedzmore.util.flowFromMessageApi
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import java.util.UUID
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

	override fun getAllFeed(request: GetAllFeedRequest): Flow<ResultState<FeedsPostPaginationResponse>> =
		flowFromApi {
			feedApi.getFeed(request.page, request.size)
		}

	override fun getExplorePosts(request: GetAllFeedRequest): Flow<ResultState<FeedsPostPaginationResponse>> =
		flowFromApi {
			feedApi.getExplorePosts(request.page, request.size)
		}

	override fun getFeedByUsername(request: GetFeedByUsernameRequest): Flow<ResultState<FeedsPostPaginationResponse>> =
		flowFromApi {
			feedApi.getFeedByUsername(
				request.username,
				request.page,
				request.size
			)
		}

	override fun likePost(postId: UUID): Flow<ResultState<String>> =
		flowFromMessageApi {
			feedApi.likePost(postId)
		}

	override fun dislikePost(postId: UUID): Flow<ResultState<String>> =
		flowFromMessageApi {
			feedApi.dislikePost(postId)
		}

	override fun addComment(request: AddCommentRequest): Flow<ResultState<String>> =
		flowFromMessageApi {
			feedApi.addComment(
				postId = request.postId,
				request = request
			)
		}

	override fun getComment(request: GetCommentRequest): Flow<ResultState<PaginationResponseWrapper<CommentItem>>> =
		flowFromApi {
			feedApi.getPostComment(
				postId = request.postId,
				page = request.page
			)
		}
}