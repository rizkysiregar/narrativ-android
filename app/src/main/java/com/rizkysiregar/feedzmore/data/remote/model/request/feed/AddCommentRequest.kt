package com.rizkysiregar.feedzmore.data.remote.model.request.feed

import java.util.UUID

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/03/26
 * Description: Software Engineer
 */

data class AddCommentRequest(
	val postId: UUID,
	val content: String
)