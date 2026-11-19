package com.rizkysiregar.narrativ.ui.main.navigation.routes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 21/11/25
 * Description: Software Engineer
 */

sealed class HomePathRoute(val route: String) {
	object Feed : MainPathRoute("feed_post")
	object UploadPost : MainPathRoute("upload-post")
	/** A post's thread: the post with its comments under it. */
	object PostDetail : HomePathRoute("post_detail/{postId}?openComments={openComments}&commentId={commentId}") {
		const val ARG_POST_ID = "postId"
		const val ARG_OPEN_COMMENTS = "openComments"
		const val ARG_COMMENT_ID = "commentId"

		/**
		 * [openComments] lands on the comments with the input ready, for the comment button or a push about a
		 * comment; [commentId] scrolls to that comment and marks it.
		 */
		fun create(postId: String, openComments: Boolean = false, commentId: String? = null): String =
			"post_detail/$postId?openComments=$openComments" + commentId?.let { "&commentId=$it" }.orEmpty()
	}

	/** A post's media full screen. */
	object PostMedia : HomePathRoute("post_media/{postId}?page={page}") {
		const val ARG_POST_ID = "postId"
		const val ARG_PAGE = "page"

		fun create(postId: String, page: Int = 0): String = "post_media/$postId?page=$page"
	}
	object Profile : MainPathRoute("profile")
	object Settings : HomePathRoute("settings")
	object Notifications : HomePathRoute("notifications")
	object BlockedUsers : HomePathRoute("blocked_users")
	object ChangeEmail : HomePathRoute("change_email")

	object FollowList : HomePathRoute("follow_list/{userId}/{type}") {
		const val ARG_USER_ID = "userId"
		const val ARG_TYPE = "type"
		fun create(userId: String, type: String): String = "follow_list/$userId/$type"
	}

	object UserDetail : HomePathRoute("user_detail/{userId}/{username}") {
		const val ARG_USER_ID = "userId"
		const val ARG_USERNAME = "username"

		fun create(userId: String, username: String): String = "user_detail/$userId/$username"
	}
}