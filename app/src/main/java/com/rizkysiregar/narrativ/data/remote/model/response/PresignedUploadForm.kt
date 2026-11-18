package com.rizkysiregar.narrativ.data.remote.model.response

/**
 * A presigned POST upload: a multipart/form-data POST to [url] with every entry of [fields] as a form field and the
 * file last. The signed policy pins the object key, the content type and the maximum size, so storage refuses a file
 * that is too large before it is stored, which a presigned PUT cannot do.
 */
data class PresignedUploadForm(
	val url: String,
	val fields: Map<String, String>
)
