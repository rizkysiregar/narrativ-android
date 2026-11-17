package com.rizkysiregar.narrativ.data.remote.model.request.post

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Request body to create a post from already uploaded objects. A post needs a
 * caption (max 500 chars) or at least one media key; up to 5 keys, in display order.
 */
data class AddPostRequest(
	val caption: String?,
	val mediaKeys: List<String>
)
