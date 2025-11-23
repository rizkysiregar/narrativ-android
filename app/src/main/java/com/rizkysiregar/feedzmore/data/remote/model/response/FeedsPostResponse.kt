package com.rizkysiregar.feedzmore.data.remote.model.response

import com.google.gson.annotations.SerializedName

data class FeedsPostResponse(

	@field:SerializedName("pageNumber")
	val pageNumber: Int,

	@field:SerializedName("last")
	val last: Boolean,

	@field:SerializedName("totalPages")
	val totalPages: Int,

	@field:SerializedName("pageSize")
	val pageSize: Int,

	@field:SerializedName("content")
	val content: List<FeedContentItem>,

	@field:SerializedName("totalElements")
	val totalElements: Int
)

data class FeedContentItem(

	@field:SerializedName("createdAt")
	val createdAt: String,

	@field:SerializedName("imageUrl")
	val imageUrl: String,

	@field:SerializedName("caption")
	val caption: String,

	@field:SerializedName("likeCount")
	val likeCount: Int,

	@field:SerializedName("id")
	val id: String,

	@field:SerializedName("liked")
	val liked: Boolean,

	@field:SerializedName("username")
	val username: String,

	@field:SerializedName("commentCount")
	val commentCount: Int
)
