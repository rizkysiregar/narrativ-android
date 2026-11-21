package com.rizkysiregar.narrativ.data.repository.impl

import com.rizkysiregar.narrativ.data.remote.api.PostApi
import com.rizkysiregar.narrativ.data.remote.model.request.post.AddPostRequest
import com.rizkysiregar.narrativ.data.remote.model.request.post.CreatePostUploadUrlRequest
import com.rizkysiregar.narrativ.data.remote.model.response.post.CreatePostResponse
import com.rizkysiregar.narrativ.data.remote.model.response.post.CreatePostUploadUrlResponse
import com.rizkysiregar.narrativ.data.repository.PostRepository
import retrofit2.Retrofit
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
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

	override suspend fun createPost(caption: String, mediaKeys: List<String>): CreatePostResponse {
		val response = postApi.createPost(
			AddPostRequest(caption = caption.takeIf { it.isNotBlank() }, mediaKeys = mediaKeys)
		)
		return response.data ?: throw IllegalStateException(response.message)
	}
}
