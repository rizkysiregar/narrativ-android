package com.rizkysiregar.feedzmore.nav

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */
sealed class Route(val route: String) {

	// onboard page
	object Splash : Route("splash")
	object Auth : Route("auth")
	object Login : Route("login")
	object Register : Route("register")


	// post login page
	object Home : Route("home")
	object UploadPost : Route("upload-post")
	object PostDetail : Route("post-detail")
	object SearchPage : Route("search-page")
	object Profile : Route("profile")



}