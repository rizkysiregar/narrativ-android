package com.rizkysiregar.feedzmore.ui.feature.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedRoute
import com.rizkysiregar.feedzmore.ui.feature.home.post.PostFeedRoute
import com.rizkysiregar.feedzmore.ui.feature.home.profile.ProfileRoute
import com.rizkysiregar.feedzmore.ui.feature.home.search.SearchRoute
import com.rizkysiregar.feedzmore.ui.feature.home.userdetail.UserDetailRoute
import com.rizkysiregar.feedzmore.ui.main.navigation.BottomNavScreen
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.AuthPathRoutes
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.HomePathRoute

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
	var feedRefreshVersion by remember { mutableIntStateOf(0) }
	val currentRoute = homeNavController.currentBackStackEntryAsState().value?.destination?.route

	Scaffold(
		bottomBar = {
			if (currentRoute != HomePathRoute.UserDetail.route) {
				BottomBar(homeNavController)
			}
		}
	) { paddingValues ->
		NavHost(
			navController = homeNavController,
			route = Constants.HOME_GRAPH,
			startDestination = BottomNavScreen.Feed.route,
			modifier = Modifier.padding(paddingValues)
		) {
			composable(BottomNavScreen.Feed.route) {
				FeedRoute(
					refreshVersion = feedRefreshVersion,
					onCreateFeed = {
						homeNavController.navigate(BottomNavScreen.PostFeed.route) {
							launchSingleTop = true
						}
					}
				)
			}
			composable(BottomNavScreen.Search.route) {
				SearchRoute(
					onUserClick = { userId, username ->
						homeNavController.navigate(HomePathRoute.UserDetail.create(userId, username))
					}
				)
			}
			composable(BottomNavScreen.PostFeed.route) {
				PostFeedRoute(
					onPostSuccess = {
						feedRefreshVersion += 1
						homeNavController.navigate(BottomNavScreen.Feed.route) {
							popUpTo(BottomNavScreen.PostFeed.route) {
								inclusive = true
							}
							launchSingleTop = true
						}
					}
				)
			}
			composable(BottomNavScreen.Profile.route) {
				ProfileRoute(
					onLogoutClick = {
						navController.navigate(AuthPathRoutes.Login.route) {
							popUpTo(Constants.HOME_GRAPH) {
								inclusive = true
							}
							launchSingleTop = true
						}
					}
				)
			}
			composable(
				route = HomePathRoute.UserDetail.route,
				arguments = listOf(
					navArgument(HomePathRoute.UserDetail.ARG_USER_ID) { type = NavType.StringType },
					navArgument(HomePathRoute.UserDetail.ARG_USERNAME) { type = NavType.StringType }
				)
			) { entry ->
				UserDetailRoute(
					userId = entry.arguments?.getString(HomePathRoute.UserDetail.ARG_USER_ID).orEmpty(),
					username = entry.arguments?.getString(HomePathRoute.UserDetail.ARG_USERNAME).orEmpty(),
					onBackClick = { homeNavController.popBackStack() }
				)
			}
		}
	}
}
