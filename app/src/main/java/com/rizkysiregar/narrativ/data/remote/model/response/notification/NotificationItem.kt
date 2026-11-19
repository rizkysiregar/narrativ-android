package com.rizkysiregar.narrativ.data.remote.model.response.notification

/**
 * One entry of the inbox. Likes of one post on one day are a single entry: [actors] holds the newest one or two
 * people and [actorCount] how many there are in all ("ani, budi and 3 others liked your post").
 */
data class NotificationItem(
	// pass it back to mark this entry read
	val groupKey: String,
	val type: String,
	val actors: List<NotificationActor> = emptyList(),
	val actorCount: Int = 1,
	val postId: String? = null,
	// first photo of the post, or the poster of its first video; null for a text post
	val postThumbnailUrl: String? = null,
	val commentId: String? = null,
	// the start of the comment, for comments, replies and pins
	val preview: String? = null,
	val createdAt: String? = null,
	val unread: Boolean = false
) {
	companion object {
		const val TYPE_POST_LIKED = "POST_LIKED"
		const val TYPE_POST_COMMENTED = "POST_COMMENTED"
		const val TYPE_COMMENT_REPLIED = "COMMENT_REPLIED"
		const val TYPE_FOLLOWED = "FOLLOWED"
		const val TYPE_COMMENT_PINNED = "COMMENT_PINNED"
	}
}

data class NotificationActor(
	val id: String,
	val username: String,
	val profileImageUrl: String? = null
)

data class NotificationPage(
	val content: List<NotificationItem> = emptyList(),
	val last: Boolean = true,
	// pass it back as ?cursor= for the next page; null on the last page
	val nextCursor: String? = null
)

data class UnreadCountResponse(val unread: Int = 0)
