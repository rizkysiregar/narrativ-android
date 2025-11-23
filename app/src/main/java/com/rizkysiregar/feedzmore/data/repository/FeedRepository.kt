package com.rizkysiregar.feedzmore.data.repository

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.GetFeedPostRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.FeedsPostResponse
import kotlinx.coroutines.flow.Flow

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */
interface FeedRepository {
	fun getFeed(feedRequest: GetFeedPostRequest): Flow<ResultState<FeedsPostResponse>>
}