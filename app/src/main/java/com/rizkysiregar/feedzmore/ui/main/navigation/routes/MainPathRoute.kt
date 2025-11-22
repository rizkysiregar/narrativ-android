package com.rizkysiregar.feedzmore.ui.main.navigation.routes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */
sealed class MainPathRoute(val route: String) {

	// onboard page
	object Splash : MainPathRoute("splash")
	object Auth : MainPathRoute("auth")
	object Login : MainPathRoute("login")
	object Register : MainPathRoute("register")

}