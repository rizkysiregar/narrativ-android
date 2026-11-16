package com.rizkysiregar.narrativ.data.remote.model.request.feed

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

data class GetAllFeedRequest(
	val page: Int,
	val size: Int,
	// explore only: leave out caption only posts (grid view)
	val mediaOnly: Boolean = false,
)