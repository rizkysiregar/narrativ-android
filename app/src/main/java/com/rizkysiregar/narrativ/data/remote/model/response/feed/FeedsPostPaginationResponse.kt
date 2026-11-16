package com.rizkysiregar.narrativ.data.remote.model.response.feed

import com.google.gson.annotations.SerializedName

enum class MediaType { IMAGE, VIDEO }

/** One image or video of a post, in display order. [url] and [thumbnailUrl] are presigned and expire. */
data class PostMediaItem(

	@field:SerializedName("id")
	val id: String,

	@field:SerializedName("type")
	val type: MediaType,

	@field:SerializedName("url")
	val url: String?,

	@field:SerializedName("thumbnailUrl")
	val thumbnailUrl: String?
) {
	val isVideo: Boolean
		get() = type == MediaType.VIDEO
}

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

	// Empty for a caption-only post. Nullable because Gson skips the Kotlin default when it rebuilds
	// a post from JSON (post detail), so always read it through [mediaItems].
	@field:SerializedName("media")
	val media: List<PostMediaItem>? = null,

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

	// the author, so the post can be reported and the author blocked; null for an older server
	@field:SerializedName("userId")
	val userId: String? = null,

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

	/** Media in display order; empty for a caption-only post and for a taken down post seen by others. */
	val mediaItems: List<PostMediaItem>
		get() = media.orEmpty()

	val hasMedia: Boolean
		get() = mediaItems.isNotEmpty()

	val hasVideo: Boolean
		get() = mediaItems.any { it.isVideo }

	/** The video shown on [page], or null when that page is an image (or out of range). */
	fun videoAt(page: Int): PostMediaItem? = mediaItems.getOrNull(page)?.takeIf { it.isVideo }

	/** Shown in place of the media when the post is taken down. */
	val takedownMessage: String
		get() = takedownReason?.takeIf { it.isNotBlank() } ?: DEFAULT_TAKEDOWN_MESSAGE
}

const val MODERATION_TAKEN_DOWN = "TAKEN_DOWN"
const val DEFAULT_TAKEDOWN_MESSAGE = "This post was taken down for violating our community rules."
