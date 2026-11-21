package com.rizkysiregar.narrativ.data.remote.model.request.feed

import java.util.UUID

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/03/26
 */

data class AddCommentRequest(
	val postId: UUID,
	val content: String,
	// the comment being answered; a reply to a reply is flattened to one level by the server
	val parentId: UUID? = null,
	// words of the post's caption a top-level comment answers; the server checks they are in the caption
	val quoteText: String? = null
)