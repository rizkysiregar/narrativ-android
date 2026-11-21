package com.rizkysiregar.narrativ.data.remote.model.request.feed

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 */

data class GetFeedPostRequest(
	val page: Int,
	val size: Int,
	val userId: String
)