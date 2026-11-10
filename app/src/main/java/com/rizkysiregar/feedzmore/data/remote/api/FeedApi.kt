package com.rizkysiregar.feedzmore.data.remote.api

import com.rizkysiregar.feedzmore.data.remote.model.request.feed.AddCommentRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.LikeFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedsPostPaginationResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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

	@GET("posts/explore")
	suspend fun getExplorePosts(
		@Query("page") page: Int,
		@Query("size") size: Int,
		@Query("mediaOnly") mediaOnly: Boolean = false
	): BaseResponse<FeedsPostPaginationResponse>

	@GET("posts/users/{username}/posts")
	suspend fun getFeedByUsername(
		@Path("username") username: String,
		@Query("page") page: Int,
		@Query("size") size: Int,
		@Query("mediaOnly") mediaOnly: Boolean = false
	): BaseResponse<FeedsPostPaginationResponse>

	@POST("posts/{postId}/like")
	suspend fun likePost(
		@Path("postId") postId: UUID
	): BaseResponse<String>

	@DELETE("posts/{postId}/dislike")
	suspend fun dislikePost(
		@Path("postId") postId: UUID
	): BaseResponse<String>

	@POST("posts/{postId}/comments")
	suspend fun addComment(
		@Body request: AddCommentRequest,
		@Path("postId") postId: UUID
	): BaseResponse<String>

	@GET("posts/{postId}/comments")
	suspend fun getPostComment(
		@Path("postId") postId: UUID,
		@Query("page") page: Int
	): BaseResponse<PaginationResponseWrapper<CommentItem>>
}