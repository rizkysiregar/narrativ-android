package com.rizkysiregar.feedzmore.ui.main.navigation.routes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 21/11/25
 * Description: Software Engineer
 */

sealed class HomePathRoute(val route: String) {
	object Feed : MainPathRoute("feed_post")
	object UploadPost : MainPathRoute("upload-post")
	object PostDetail : MainPathRoute("post-detail")
	object SearchPage : MainPathRoute("search-page")
	object Profile : MainPathRoute("profile")
}