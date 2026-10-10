package com.rizkysiregar.feedzmore.data.remote.model.response.feed

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 06/03/26
 * Description: Software Engineer
 */

data class CommentItem(
	val username: String,
	val profileImageUrl: String,
	val commentContent: String
)