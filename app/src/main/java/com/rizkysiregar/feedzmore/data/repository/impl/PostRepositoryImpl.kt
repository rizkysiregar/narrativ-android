package com.rizkysiregar.feedzmore.data.repository.impl

import com.rizkysiregar.feedzmore.data.remote.api.PostApi
import com.rizkysiregar.feedzmore.data.remote.model.request.post.AddPostRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.post.CreatePostUploadUrlRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.post.CreatePostResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.post.CreatePostUploadUrlResponse
import com.rizkysiregar.feedzmore.data.repository.PostRepository
import retrofit2.Retrofit
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Software Engineer
 */
class PostRepositoryImpl @Inject constructor(
	private val retrofit: Retrofit
) : PostRepository {

	private val postApi: PostApi by lazy {
		retrofit.create(PostApi::class.java)
	}

	override suspend fun createUploadUrl(contentType: String): CreatePostUploadUrlResponse {
		val response = postApi.createUploadUrl(CreatePostUploadUrlRequest(contentType))
		return response.data ?: throw IllegalStateException(response.message)
	}

	override suspend fun createPost(caption: String, objectKey: String): CreatePostResponse {
		val response = postApi.createPost(AddPostRequest(caption = caption, objectKey = objectKey))
		return response.data ?: throw IllegalStateException(response.message)
	}
}
