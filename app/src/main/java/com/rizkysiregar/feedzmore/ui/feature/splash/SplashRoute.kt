package com.rizkysiregar.feedzmore.ui.feature.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.AuthPathRoutes
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

	// launched effect only react when isLoggedIn is not null
	LaunchedEffect(isLoggedIn) {
		when (isLoggedIn) {
			true -> {
				// navigate to home
				navController.navigate(Constants.HOME_GRAPH) {
					popUpTo(MainPathRoute.Splash.route) { inclusive = true }
				}
			}

			false -> {
				navController.navigate(AuthPathRoutes.Login.route) {
					popUpTo(MainPathRoute.Splash.route) { inclusive = true }
				}
			}

			null -> {
				// Do nothing, just wait for real value from datastore coming.
			}
		}
	}

	// splash UI
	SplashScreen()
}