package com.rizkysiregar.feedzmore.data.repository

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetFeedPostRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.PostingFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.FeedsPostResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.PostingFeedResponse
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */
interface FeedRepository {
	fun getFeed(feedRequest: GetFeedPostRequest): Flow<ResultState<FeedsPostResponse>>
	fun postingFeed(postingFeedRequest: PostingFeedRequest): Flow<ResultState<PostingFeedResponse>>
	fun likePost(postId: UUID): Flow<ResultState<String>>
	fun dislikePost(postId: UUID): Flow<ResultState<String>>
}