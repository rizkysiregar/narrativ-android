package com.rizkysiregar.feedzmore.data.repository

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.AddCommentRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetCommentRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetRepliesRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetAllFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetFeedByUsernameRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedsPostPaginationResponse
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */
interface FeedRepository {
	fun getAllFeed(request: GetAllFeedRequest): Flow<ResultState<FeedsPostPaginationResponse>>
	fun getExplorePosts(request: GetAllFeedRequest): Flow<ResultState<FeedsPostPaginationResponse>>
	fun getFeedByUsername(request: GetFeedByUsernameRequest): Flow<ResultState<FeedsPostPaginationResponse>>
	fun likePost(postId: UUID): Flow<ResultState<String>>
	fun dislikePost(postId: UUID): Flow<ResultState<String>>
	fun addComment(request: AddCommentRequest): Flow<ResultState<String>>
	fun deleteComment(commentId: UUID): Flow<ResultState<String>>
	fun getComment(request: GetCommentRequest): Flow<ResultState<PaginationResponseWrapper<CommentItem>>>
	fun getReplies(request: GetRepliesRequest): Flow<ResultState<PaginationResponseWrapper<CommentItem>>>
}