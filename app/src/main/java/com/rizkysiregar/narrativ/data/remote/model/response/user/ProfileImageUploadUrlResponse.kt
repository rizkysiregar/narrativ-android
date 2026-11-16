package com.rizkysiregar.narrativ.data.remote.model.response.user

/**
 * Presigned URL details for a direct-to-storage profile picture upload.
 */
data class ProfileImageUploadUrlResponse(
	val presignedUrl: String,
	val objectKey: String,
	val expiresInSeconds: Long
)
