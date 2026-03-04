package com.rizkysiregar.feedzmore.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.ui.feature.auth.login.LoginRoute
import com.rizkysiregar.feedzmore.ui.feature.auth.regiter.RegisterRoute
import com.rizkysiregar.feedzmore.ui.feature.auth.reset.ResetPasswordRoute
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.AuthPathRoutes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 19/11/25
 * Description: Software Engineer
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
			onRegisterSuccess = {
				navController.navigate(AuthPathRoutes.Login.route) {
					popUpTo(AuthPathRoutes.Register.route) { inclusive = true }
				}
			}
		)
	}

	composable(AuthPathRoutes.ResetPassword.route) {
		ResetPasswordRoute(navController)
	}


}