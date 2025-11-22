package com.rizkysiregar.feedzmore.ui.main

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.MainPathRoute
import com.rizkysiregar.feedzmore.ui.main.navigation.authGraph
import com.rizkysiregar.feedzmore.ui.main.navigation.homeGraph
import com.rizkysiregar.feedzmore.ui.main.navigation.splashGraph

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */


@Composable
fun MainRoot() {
	val navController = rememberNavController()

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