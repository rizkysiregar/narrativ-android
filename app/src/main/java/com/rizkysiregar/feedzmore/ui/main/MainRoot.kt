package com.rizkysiregar.feedzmore.ui.main

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.MainPathRoute
import com.rizkysiregar.feedzmore.ui.main.navigation.authGraph
import com.rizkysiregar.feedzmore.ui.main.navigation.homeGraph
import com.rizkysiregar.feedzmore.ui.main.navigation.splashGraph
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.AuthPathRoutes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */


@Composable
fun MainRoot(
	viewModel: MainViewModel = hiltViewModel()
) {
	val navController = rememberNavController()
	val context = LocalContext.current
	val sessionExpiredMessage = viewModel.sessionExpiredMessage.collectAsStateWithLifecycle()

	LaunchedEffect(sessionExpiredMessage.value) {
		sessionExpiredMessage.value?.let { message ->
			Toast.makeText(context, message, Toast.LENGTH_LONG).show()
			navController.navigate(AuthPathRoutes.Login.route) {
				popUpTo(Constants.MAIN_GRAPH) { inclusive = false }
				launchSingleTop = true
			}
		}
	}

	NavHost(
		navController = navController,
		startDestination = MainPathRoute.Splash.route,
		route = Constants.MAIN_GRAPH
	) {
		splashGraph(navController)
		authGraph(navController)
		homeGraph(navController)
	}
}
