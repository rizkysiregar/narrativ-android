package com.rizkysiregar.narrativ.ui.main.navigation.routes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 21/11/25
 * Description: Software Engineer
 */

sealed class HomePathRoute(val route: String) {
	object Feed : MainPathRoute("feed_post")
	object UploadPost : MainPathRoute("upload-post")
	object PostDetail : HomePathRoute("post_detail/{postId}?openComments={openComments}") {
		const val ARG_POST_ID = "postId"
		const val ARG_OPEN_COMMENTS = "openComments"

		/** [openComments] opens the comment sheet on arrival, for a push about a comment or reply. */
		fun create(postId: String, openComments: Boolean = false): String =
			"post_detail/$postId?openComments=$openComments"
	}
	object Profile : MainPathRoute("profile")
	object Settings : HomePathRoute("settings")
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