package com.rizkysiregar.feedzmore.ui.main.navigation.routes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */
sealed class MainPathRoute(val route: String) {

	// onboard page
	object Splash : MainPathRoute("splash")

	// first-launch intro, shown once before login
	object Onboarding : MainPathRoute("onboarding")

	// privacy policy and terms, shown from Settings and from the Register screen
	object Legal : MainPathRoute("legal/{doc}") {
		const val ARG_DOC = "doc"

		fun create(path: String): String = "legal/$path"
	}
}