package com.rizkysiregar.feedzmore.ui.feature.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedRoute
import com.rizkysiregar.feedzmore.ui.feature.home.profile.ProfileRoute
import com.rizkysiregar.feedzmore.ui.feature.home.search.SearchRoute
import com.rizkysiregar.feedzmore.ui.main.navigation.BottomNavScreen

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 19/11/25
 * Description: Software Engineer
 */


@Composable
fun HomeRoot() {
	val homeNavController = rememberNavController()

	Scaffold(
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
			composable(BottomNavScreen.Profile.route) { ProfileRoute() }
		}
	}
}