package com.rizkysiregar.feedzmore.data.repository

import com.rizkysiregar.feedzmore.data.remote.model.response.post.CreatePostResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.post.CreatePostUploadUrlResponse

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Backend operations for the presigned post media upload flow.
 */
interface PostRepository {

	/**
	 * Step 1: request a presigned URL + objectKey for the given content type.
	 * Throws [retrofit2.HttpException] on non-2xx responses.
	 */
	suspend fun createUploadUrl(contentType: String): CreatePostUploadUrlResponse

	/**
	 * Step 3: persist the post once the media has been uploaded to storage.
	 * Throws [retrofit2.HttpException] on non-2xx responses.
	 */
	suspend fun createPost(caption: String, objectKey: String): CreatePostResponse
}
