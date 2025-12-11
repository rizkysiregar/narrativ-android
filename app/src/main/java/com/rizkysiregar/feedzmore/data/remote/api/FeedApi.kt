package com.rizkysiregar.feedzmore.data.remote.api

import com.rizkysiregar.feedzmore.data.remote.model.request.PostingFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.FeedsPostResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.PostingFeedResponse
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */
interface FeedApi {
	@GET("posts")
	suspend fun getFeed(
		@Query("page") page: Int,
		@Query("size") size: Int,
		@Query("userId") userId: String
	): BaseResponse<FeedsPostResponse>

	@Multipart
	@POST("posts/upload")
	suspend fun postingFeed(
		@Part("addPostRequest") addPostRequest: PostingFeedRequest,
		@Part image: MultipartBody.Part
	): BaseResponse<PostingFeedResponse>

}