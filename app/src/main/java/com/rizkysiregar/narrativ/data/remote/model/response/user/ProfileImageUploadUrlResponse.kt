package com.rizkysiregar.narrativ.data.remote.model.response.user

import com.rizkysiregar.narrativ.data.remote.model.response.PresignedUploadForm

/**
 * Presigned URL details for a direct-to-storage profile picture upload.
 */
data class ProfileImageUploadUrlResponse(
	val presignedUrl: String,
	val objectKey: String,
	val expiresInSeconds: Long,
	// preferred over presignedUrl: storage caps its size. null from a server that only hands out PUT urls
	val uploadForm: PresignedUploadForm? = null
)
