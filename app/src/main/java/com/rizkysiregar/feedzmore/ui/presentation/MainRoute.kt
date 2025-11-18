package com.rizkysiregar.feedzmore.ui.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.rizkysiregar.feedzmore.nav.Route

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */


@Composable
fun MainRoute() {
	val navController = rememberNavController()

	NavHost(
		navController = navController,
		startDestination = Route.Splash.route
	) {

	}
}