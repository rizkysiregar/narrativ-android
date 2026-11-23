package com.rizkysiregar.narrativ.data.remote.model.request.feed

/**
 * One page of a post search: [query] is what was typed (2 to 50 characters), [cursor] the nextCursor of the previous
 * page, null for the first one.
 */
data class SearchPostsRequest(
	val query: String,
	val size: Int,
	val cursor: String? = null
)
