package com.rizkysiregar.narrativ.ui.main.navigation.routes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 27/02/26
 * Description: Software Engineer
 */
sealed class AuthPathRoutes(val route: String) {
	object Login : AuthPathRoutes("login")
	object Register : AuthPathRoutes("register")
	object ResetPassword : AuthPathRoutes("reset_password")
}
