package com.rizkysiregar.narrativ.data.remote.api

import com.rizkysiregar.narrativ.data.remote.model.request.feed.AddCommentRequest
import com.rizkysiregar.narrativ.data.remote.model.request.feed.PinCommentRequest
import com.rizkysiregar.narrativ.data.remote.model.request.feed.LikeFeedRequest
import com.rizkysiregar.narrativ.data.remote.model.response.BaseResponse
import com.rizkysiregar.narrativ.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.narrativ.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedsPostPaginationResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 */
interface FeedApi {

	// The feeds take the nextCursor of the previous page as cursor (the server then ignores page). A null cursor
	// is left out of the url, so the first page goes by page=0.
	@GET("posts")
	suspend fun getFeed(
		@Query("page") page: Int,
		@Query("size") size: Int,
		@Query("cursor") cursor: String? = null
	): BaseResponse<FeedsPostPaginationResponse>

	@GET("posts/explore")
	suspend fun getExplorePosts(
		@Query("page") page: Int,
		@Query("size") size: Int,
		@Query("mediaOnly") mediaOnly: Boolean = false,
		@Query("cursor") cursor: String? = null
	): BaseResponse<FeedsPostPaginationResponse>

	@GET("posts/users/{username}/posts")
	suspend fun getFeedByUsername(
		@Path("username") username: String,
		@Query("page") page: Int,
		@Query("size") size: Int,
		@Query("mediaOnly") mediaOnly: Boolean = false,
		@Query("cursor") cursor: String? = null
	): BaseResponse<FeedsPostPaginationResponse>

	@GET("posts/{postId}")
	suspend fun getPost(
		@Path("postId") postId: UUID
	): BaseResponse<FeedContentItem>

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

	@DELETE("comments/{commentId}")
	suspend fun deleteComment(
		@Path("commentId") commentId: UUID
	): BaseResponse<String>

	@GET("posts/{postId}/comments")
	suspend fun getPostComment(
		@Path("postId") postId: UUID,
		@Query("page") page: Int
	): BaseResponse<PaginationResponseWrapper<CommentItem>>

	@PUT("posts/{postId}/pinned-comment")
	suspend fun pinComment(
		@Path("postId") postId: UUID,
		@Body request: PinCommentRequest
	): BaseResponse<String>

	@DELETE("posts/{postId}/pinned-comment")
	suspend fun unpinComment(
		@Path("postId") postId: UUID
	): BaseResponse<String>

	@GET("comments/{commentId}/replies")
	suspend fun getCommentReplies(
		@Path("commentId") commentId: UUID,
		@Query("page") page: Int
	): BaseResponse<PaginationResponseWrapper<CommentItem>>
}
