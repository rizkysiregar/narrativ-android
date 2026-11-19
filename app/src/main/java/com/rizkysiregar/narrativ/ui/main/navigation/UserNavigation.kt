package com.rizkysiregar.narrativ.ui.main.navigation

import androidx.navigation.NavController
import com.rizkysiregar.narrativ.ui.main.navigation.routes.HomePathRoute

/**
 * Where opening a user leads. Your own account is the Profile tab, never a user detail with a Follow button on it,
 * the same as tapping yourself in Instagram or Threads. [currentUserId] is blank until the session is read, then
 * every user (yourself too) gets the detail, which hides its Follow button for its own account anyway.
 */
fun userRoute(userId: String, username: String, currentUserId: String): String =
	if (currentUserId.isNotBlank() && userId == currentUserId) BottomNavScreen.Profile.route
	else HomePathRoute.UserDetail.create(userId, username)

/** Opens the user with [userId]; see [userRoute]. */
fun NavController.openUser(userId: String, username: String, currentUserId: String) {
	val route = userRoute(userId, username, currentUserId)
	if (route == BottomNavScreen.Profile.route) navigateToTab(BottomNavScreen.Profile) else navigate(route)
}

/** Switches to a bottom bar tab, keeping the state of the tab that is left, as the bottom bar does. */
fun NavController.navigateToTab(item: BottomNavScreen) {
	navigate(item.route) {
		launchSingleTop = true
		restoreState = true
		popUpTo(graph.startDestinationId) {
			saveState = true
		}
	}
}
