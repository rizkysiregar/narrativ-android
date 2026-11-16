package com.rizkysiregar.narrativ.data.remote.model.response.post

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Presigned URL details for a direct-to-storage media upload.
 */
data class CreatePostUploadUrlResponse(
	val presignedUrl: String,
	val objectKey: String,
	val expiresInSeconds: Long
)
