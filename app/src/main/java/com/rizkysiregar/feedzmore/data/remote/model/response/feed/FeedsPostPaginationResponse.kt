package com.rizkysiregar.feedzmore.data.remote.model.response.feed

import com.google.gson.annotations.SerializedName
import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse

enum class MediaType { IMAGE, VIDEO }

data class FeedsPostPaginationResponse(

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

	@field:SerializedName("mediaUrl")
	val mediaUrl: String,

	@field:SerializedName("mediaType")
	val mediaType: MediaType,

	@field:SerializedName("thumbnailUrl")
	val thumbnailUrl: String?,

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
	val commentCount: Int,

	@field:SerializedName("moderationStatus")
	val moderationStatus: String? = null
) {
	val isTakenDown: Boolean
		get() = moderationStatus == MODERATION_TAKEN_DOWN
}

const val MODERATION_TAKEN_DOWN = "TAKEN_DOWN"

/**
 * Taken-down posts come back with null media/caption, which the UI cannot render,
 * so they are dropped here before reaching the presentation layer.
 */
fun FeedsPostPaginationResponse.withoutTakenDown(): FeedsPostPaginationResponse =
	copy(content = content.filterNot { it.isTakenDown })

fun BaseResponse<FeedsPostPaginationResponse>.withoutTakenDown(): BaseResponse<FeedsPostPaginationResponse> =
	copy(data = data?.withoutTakenDown())
