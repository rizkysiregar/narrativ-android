package com.rizkysiregar.narrativ.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.rizkysiregar.narrativ.ui.main.navigation.routes.MainPathRoute
import com.rizkysiregar.narrativ.ui.feature.splash.SplashRoute

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */


fun NavGraphBuilder.splashGraph(navController: NavHostController) {
	composable(MainPathRoute.Splash.route) {
		SplashRoute(navController)
	}
}