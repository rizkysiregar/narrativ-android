package com.rizkysiregar.feedzmore.data.remote.model.response.feed

import com.google.gson.annotations.SerializedName

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
	val mediaUrl: String?,

	@field:SerializedName("mediaType")
	val mediaType: MediaType,

	@field:SerializedName("thumbnailUrl")
	val thumbnailUrl: String?,

	@field:SerializedName("caption")
	val caption: String?,

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
	val moderationStatus: String? = null,

	@field:SerializedName("takedownReason")
	val takedownReason: String? = null,

	@field:SerializedName("takenDownAt")
	val takenDownAt: String? = null
) {
	val isTakenDown: Boolean
		get() = moderationStatus == MODERATION_TAKEN_DOWN

	/** Shown in place of the media when the post is taken down. */
	val takedownMessage: String
		get() = takedownReason?.takeIf { it.isNotBlank() } ?: DEFAULT_TAKEDOWN_MESSAGE
}

const val MODERATION_TAKEN_DOWN = "TAKEN_DOWN"
const val DEFAULT_TAKEDOWN_MESSAGE = "This post was taken down for violating our community rules."
