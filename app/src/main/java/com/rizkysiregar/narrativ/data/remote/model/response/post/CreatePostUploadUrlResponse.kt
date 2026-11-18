package com.rizkysiregar.narrativ.data.remote.model.response.post

import com.rizkysiregar.narrativ.data.remote.model.response.PresignedUploadForm

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Presigned URL details for a direct-to-storage media upload.
 */
data class CreatePostUploadUrlResponse(
	val presignedUrl: String,
	val objectKey: String,
	val expiresInSeconds: Long,
	// preferred over presignedUrl: storage caps its size. null from a server that only hands out PUT urls
	val uploadForm: PresignedUploadForm? = null
)
