package com.rizkysiregar.feedzmore.data.remote.model.request.post

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Request body to create a post from an already uploaded object.
 */
data class AddPostRequest(
	val caption: String,
	val objectKey: String
)
