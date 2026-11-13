package com.rizkysiregar.feedzmore.ui.main.navigation.routes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 21/11/25
 * Description: Software Engineer
 */

sealed class HomePathRoute(val route: String) {
	object Feed : MainPathRoute("feed_post")
	object UploadPost : MainPathRoute("upload-post")
	object PostDetail : HomePathRoute("post_detail/{postId}") {
		const val ARG_POST_ID = "postId"

		fun create(postId: String): String = "post_detail/$postId"
	}
	object Profile : MainPathRoute("profile")
	object Settings : HomePathRoute("settings")

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