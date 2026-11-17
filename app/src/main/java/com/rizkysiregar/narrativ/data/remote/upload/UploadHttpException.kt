package com.rizkysiregar.narrativ.data.remote.upload

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Non-2xx response while uploading directly to object storage.
 */
class UploadHttpException(
	val code: Int,
	val retryAfterSeconds: Long? = null
) : Exception("Upload failed with HTTP $code")
