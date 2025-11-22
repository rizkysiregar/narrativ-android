package com.rizkysiregar.feedzmore.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.HomePathRoute
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.MainPathRoute
import com.rizkysiregar.feedzmore.ui.feature.auth.AuthRoute

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 19/11/25
 * Description: Software Engineer
 */


fun NavGraphBuilder.authGraph(navController: NavHostController) {
	composable(MainPathRoute.Auth.route) {
		AuthRoute(
			navController,
			onLoginSuccess = {
				navController.navigate(Constants.HOME_GRAPH) {
					popUpTo(MainPathRoute.Auth.route) { inclusive = true }
				}
			}
		)
	}
}