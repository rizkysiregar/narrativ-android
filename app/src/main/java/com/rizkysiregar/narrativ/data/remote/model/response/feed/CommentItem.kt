package com.rizkysiregar.narrativ.data.remote.model.response.feed

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 06/03/26
 * Description: Software Engineer
 *
 * A comment is either top-level ([parentId] == null) or a reply. Replies are one level deep:
 * [parentId] is always the top-level comment, [replyToCommentId] / [replyToUsername] say which
 * comment (and whose author) was actually answered, so the UI can show the @tag.
 */
data class CommentItem(
	val id: String,
	val username: String,
	// the author, so the comment can be reported and the author blocked
	val userId: String? = null,
	val profileImageUrl: String? = null,
	// null for everybody but the author when the comment is taken down
	val commentContent: String? = null,
	val createdAt: String? = null,
	val parentId: String? = null,
	val replyToCommentId: String? = null,
	val replyToUsername: String? = null,
	// visible replies of a top-level comment, always 0 for a reply
	val replyCount: Int = 0,
	val moderationStatus: String? = null,
	val takedownReason: String? = null,
	// the author and the owner of the post may delete a comment, the server decides
	val canDelete: Boolean = false,
	// shown right after sending, before the server has confirmed it; never part of a server response
	val isPending: Boolean = false
) {
	val isTakenDown: Boolean
		get() = moderationStatus == MODERATION_TAKEN_DOWN

	val isReply: Boolean
		get() = parentId != null

	/** The top-level comment this one belongs to (itself when it is top-level). */
	val rootId: String
		get() = parentId ?: id

	companion object {
		const val MODERATION_TAKEN_DOWN = "TAKEN_DOWN"
	}
}
