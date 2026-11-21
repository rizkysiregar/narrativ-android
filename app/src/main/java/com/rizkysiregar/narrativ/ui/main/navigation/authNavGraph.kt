package com.rizkysiregar.narrativ.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.rizkysiregar.narrativ.constants.Constants
import com.rizkysiregar.narrativ.ui.feature.auth.login.LoginRoute
import com.rizkysiregar.narrativ.ui.feature.auth.register.RegisterRoute
import com.rizkysiregar.narrativ.ui.feature.auth.reset.ResetPasswordRoute
import com.rizkysiregar.narrativ.ui.main.navigation.routes.AuthPathRoutes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 19/11/25
 */


fun NavGraphBuilder.authGraph(navController: NavHostController) {
	composable(AuthPathRoutes.Login.route) {
		LoginRoute(
			navController,
			onLoginSuccess = {
				navController.navigate(Constants.HOME_GRAPH) {
					popUpTo(AuthPathRoutes.Login.route) { inclusive = true }
				}
			}
		)
	}

	composable(AuthPathRoutes.Register.route) {
		RegisterRoute(
			navController = navController,
			onRegisterSuccess = {
				navController.navigate(AuthPathRoutes.Login.route) {
					popUpTo(AuthPathRoutes.Register.route) { inclusive = true }
				}
			},
			onGoogleSignInSuccess = {
				navController.navigate(Constants.HOME_GRAPH) {
					popUpTo(AuthPathRoutes.Login.route) { inclusive = true }
				}
			}
		)
	}

	composable(AuthPathRoutes.ResetPassword.route) {
		ResetPasswordRoute(navController)
	}


}
