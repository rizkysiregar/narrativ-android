package com.rizkysiregar.feedzmore.ui.feature.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.AuthPathRoutes
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.MainPathRoute
import kotlinx.coroutines.delay

// Long enough for the intro animation to play, even though the DataStore answers in a few ms.
private const val MIN_SPLASH_MS = 1400L

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

	var minTimeElapsed by remember { mutableStateOf(false) }
	LaunchedEffect(Unit) {
		delay(MIN_SPLASH_MS)
		minTimeElapsed = true
	}

	// launched effect only react when isLoggedIn is not null
	LaunchedEffect(isLoggedIn, minTimeElapsed) {
		if (!minTimeElapsed) return@LaunchedEffect
		when (isLoggedIn) {
			true -> {
				// navigate to home
				navController.navigate(Constants.HOME_GRAPH) {
					popUpTo(MainPathRoute.Splash.route) { inclusive = true }
				}
			}

			false -> {
				// first launch gets the intro, everyone else goes straight to login
				val next = if (vm.onboardingSeen) AuthPathRoutes.Login.route else MainPathRoute.Onboarding.route
				navController.navigate(next) {
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