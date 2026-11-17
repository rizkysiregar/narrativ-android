package com.rizkysiregar.narrativ.testing.fakes

import com.rizkysiregar.narrativ.data.remote.model.response.post.CreatePostResponse
import com.rizkysiregar.narrativ.data.remote.model.response.post.CreatePostUploadUrlResponse
import com.rizkysiregar.narrativ.data.repository.PostRepository

class FakePostRepository : PostRepository {
	val uploadUrlRequests = mutableListOf<String>()
	val createdPosts = mutableListOf<Pair<String, List<String>>>()

	/** Builds the answer of the n-th createUploadUrl call (0-based); throw to fail it. */
	var onCreateUploadUrl: (call: Int) -> CreatePostUploadUrlResponse = { n ->
		CreatePostUploadUrlResponse("http://invalid.example/upload/$n", "key-$n", 900)
	}

	/** Throw to make createPost fail. */
	var onCreatePost: (caption: String, keys: List<String>) -> Unit = { _, _ -> }

	override suspend fun createUploadUrl(contentType: String): CreatePostUploadUrlResponse {
		uploadUrlRequests += contentType
		return onCreateUploadUrl(uploadUrlRequests.size - 1)
	}

	override suspend fun createPost(caption: String, mediaKeys: List<String>): CreatePostResponse {
		createdPosts += caption to mediaKeys
		onCreatePost(caption, mediaKeys)
		return CreatePostResponse("CREATED")
	}
}
