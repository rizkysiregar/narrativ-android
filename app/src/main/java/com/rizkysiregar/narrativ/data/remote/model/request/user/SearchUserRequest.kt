package com.rizkysiregar.narrativ.data.remote.model.request.user

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 * Description: Software Engineer
 */

data class SearchUserRequest(
	val username: String?,
	val page: Int,
	val size: Int
)
