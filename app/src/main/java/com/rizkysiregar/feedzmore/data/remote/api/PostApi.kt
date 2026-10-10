package com.rizkysiregar.feedzmore.data.remote.api

import com.rizkysiregar.feedzmore.data.remote.model.request.post.AddPostRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.post.CreatePostUploadUrlRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.post.CreatePostResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.post.CreatePostUploadUrlResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Endpoints for the presigned direct-to-storage post upload flow.
 */
interface PostApi {

	@POST("posts/upload-url")
	suspend fun createUploadUrl(
		@Body request: CreatePostUploadUrlRequest
	): BaseResponse<CreatePostUploadUrlResponse>

	@POST("posts")
	suspend fun createPost(
		@Body request: AddPostRequest
	): BaseResponse<CreatePostResponse>
}
