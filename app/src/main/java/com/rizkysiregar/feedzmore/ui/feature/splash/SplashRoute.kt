package com.rizkysiregar.feedzmore.ui.feature.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.HomePathRoute
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.MainPathRoute

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */


@Composable
fun SplashRoute(
	navController: NavController,
	vm: SplashViewModel = hiltViewModel()
) {
	val isLoggedIn by vm.isLoggedIn.collectAsState()
	SplashScreen(
		isLoggedIn,
		onNavigateToHome = {
			navController.navigate(Constants.HOME_GRAPH) {
				popUpTo(MainPathRoute.Splash.route) { inclusive = true }
			}
		},
		onNavigateToLogin = {
			navController.navigate(MainPathRoute.Auth.route) {
				popUpTo(MainPathRoute.Splash.route) { inclusive = true }
			}
		}
	)
}