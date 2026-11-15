package com.rizkysiregar.feedzmore.ui.main

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.data.remote.ApiErrorEvent
import com.rizkysiregar.feedzmore.ui.feature.common.ErrorDialog
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.MainPathRoute
import com.rizkysiregar.feedzmore.ui.main.navigation.authGraph
import com.rizkysiregar.feedzmore.ui.main.navigation.homeGraph
import com.rizkysiregar.feedzmore.ui.main.navigation.legalGraph
import com.rizkysiregar.feedzmore.ui.main.navigation.onboardingGraph
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

	DisposableEffect(navController) {
		val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
			destination.route?.let(viewModel::onScreenViewed)
		}
		navController.addOnDestinationChangedListener(listener)
		onDispose { navController.removeOnDestinationChangedListener(listener) }
	}

	LaunchedEffect(sessionExpiredMessage.value) {
		sessionExpiredMessage.value?.let { message ->
			Toast.makeText(context, message, Toast.LENGTH_LONG).show()
			navController.navigate(AuthPathRoutes.Login.route) {
				popUpTo(Constants.MAIN_GRAPH) { inclusive = false }
				launchSingleTop = true
			}
		}
	}

	var apiError by remember { mutableStateOf<ApiErrorEvent?>(null) }
	LaunchedEffect(Unit) {
		// Ignore events while a dialog is showing so parallel failures don't stack dialogs.
		viewModel.apiErrors.collect { if (apiError == null) apiError = it }
	}

	apiError?.let { error ->
		ErrorDialog(message = error.message, onDismiss = { apiError = null })
	}

	NavHost(
		navController = navController,
		startDestination = MainPathRoute.Splash.route,
		route = Constants.MAIN_GRAPH
	) {
		splashGraph(navController)
		onboardingGraph(navController)
		authGraph(navController)
		homeGraph(navController)
		legalGraph(navController)
	}
}
