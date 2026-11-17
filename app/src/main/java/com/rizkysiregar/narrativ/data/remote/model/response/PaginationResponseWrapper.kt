package com.rizkysiregar.narrativ.data.remote.model.response

import com.google.gson.annotations.SerializedName

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 06/03/26
 * Description: Software Engineer
 */

data class PaginationResponseWrapper<T>(

	@field:SerializedName("pageNumber")
	val pageNumber: Int,

	@field:SerializedName("last")
	val last: Boolean,

	@field:SerializedName("totalPages")
	val totalPages: Int,

	@field:SerializedName("pageSize")
	val pageSize: Int,

	@field:SerializedName("content")
	val content: List<T>,

	@field:SerializedName("totalElements")
	val totalElements: Int
)