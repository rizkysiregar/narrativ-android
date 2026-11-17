package com.rizkysiregar.narrativ.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.rizkysiregar.narrativ.constants.Constants
import com.rizkysiregar.narrativ.ui.feature.home.HomeRoot
import com.rizkysiregar.narrativ.ui.main.navigation.routes.HomePathRoute

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 19/11/25
 * Description: Software Engineer
 */


fun NavGraphBuilder.homeGraph(navHostController: NavHostController) {
	navigation(
		route = Constants.HOME_GRAPH,
		startDestination = HomePathRoute.Feed.route
	) {
		composable(HomePathRoute.Feed.route) {
			HomeRoot(navHostController)
		}
	}
}