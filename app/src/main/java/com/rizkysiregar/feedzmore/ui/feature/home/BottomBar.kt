package com.rizkysiregar.feedzmore.ui.feature.home

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.rizkysiregar.feedzmore.ui.main.navigation.BottomNavScreen
import com.rizkysiregar.feedzmore.ui.main.navigation.IconSource

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 21/11/25
 * Description: Software Engineer
 */


@Composable
fun BottomBar(
	navController: NavController
) {
	val items = listOf(
		BottomNavScreen.Feed,
		BottomNavScreen.Explore,
		BottomNavScreen.PostFeed,
		BottomNavScreen.Search,
		BottomNavScreen.Profile
	)

	NavigationBar {
		val currentDestination = navController.currentBackStackEntryAsState().value?.destination
		items.forEach { item ->
			NavigationBarItem(
				selected = currentDestination?.route == item.route,
				onClick = {
					navController.navigate(item.route) {
						launchSingleTop = true
						restoreState = true
						popUpTo(navController.graph.startDestinationId) {
							saveState = true
						}
					}
				},
				icon = {
					when (item.icon) {
						is IconSource.Vector -> {
							Icon(
								imageVector = item.icon.imageVector,
								contentDescription = item.title
							)
						}

						is IconSource.Drawable -> {
							Icon(
								painter = painterResource(item.icon.resId),
								contentDescription = item.title
							)
						}
					}
				},
				label = {
					Text(item.title)
				}
			)
		}
	}
}