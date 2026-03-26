package com.rizkysiregar.feedzmore.data.remote.api

import com.rizkysiregar.feedzmore.data.remote.model.request.feed.AddCommentRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.LikeFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.PostingFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.PostingFeedResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import java.util.UUID

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */
interface FeedApi {

	@GET("posts")
	suspend fun getFeed(
		@Query("page") page: Int,
		@Query("size") size: Int
	): BaseResponse<FeedsPostPaginationResponse>

	@GET("posts/user")
	suspend fun getFeedByUsername(
		@Query("page") page: Int,
		@Query("size") size: Int,
		@Query("username") username: String
	): BaseResponse<FeedsPostPaginationResponse>

	@Multipart
	@POST("posts/upload")
	suspend fun postingFeed(
		@Part("addPostRequest") addPostRequest: PostingFeedRequest,
		@Part image: MultipartBody.Part
	): BaseResponse<PostingFeedResponse>

	@POST("post/like")
	suspend fun likePost(
		@Body request: LikeFeedRequest
	): BaseResponse<String>

	@DELETE("post/dislike")
	suspend fun dislikePost(
		@Query("postId") postId: UUID
	): BaseResponse<String>

	@POST("comments")
	suspend fun addComment(
		@Body request: AddCommentRequest
	): BaseResponse<String>

	@GET("comments")
	suspend fun getPostComment(
		@Query("postId") postId: UUID,
		@Query("page") page: Int
	): BaseResponse<PaginationResponseWrapper<CommentItem>>
}