package com.rizkysiregar.feedzmore.ui.feature.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedRoute
import com.rizkysiregar.feedzmore.ui.feature.home.profile.ProfileRoute
import com.rizkysiregar.feedzmore.ui.feature.home.search.SearchRoute
import com.rizkysiregar.feedzmore.ui.main.navigation.BottomNavScreen
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.MainPathRoute

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 19/11/25
 * Description: Software Engineer
 */


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoot(
	navController: NavController
) {
	val homeNavController = rememberNavController()

	Scaffold(
//		topBar = {
//			TopAppBar(
//				title = { Text("Feedzmore") },
//				actions = {
//					IconButton(onClick = {
//						// ---  logout --- //
//
//
//						navController.navigate(MainPathRoute.Auth.route) {
//							popUpTo(HomePathRoute.Feed.route) {
//								inclusive = true
//							}
//							launchSingleTop = true
//						}
//					}) {
//						Icon(
//							Icons.AutoMirrored.Filled.ExitToApp,
//							contentDescription = "Exit App"
//						)
//					}
//				}
//			)
//		},
		bottomBar = {
			BottomBar(homeNavController)
		}
	) { paddingValues ->
		NavHost(
			navController = homeNavController,
			route = Constants.HOME_GRAPH,
			startDestination = BottomNavScreen.Feed.route,
			modifier = Modifier.padding(paddingValues)
		) {
			composable(BottomNavScreen.Feed.route) { FeedRoute() }
			composable(BottomNavScreen.Search.route) { SearchRoute() }
			composable(BottomNavScreen.Profile.route) {
				ProfileRoute(
					onLogoutClick = {
						navController.navigate(MainPathRoute.Auth.route) {
							popUpTo(Constants.HOME_GRAPH) {
								inclusive = true
							}
							launchSingleTop = true
						}
					}
				)
			}
		}
	}
}