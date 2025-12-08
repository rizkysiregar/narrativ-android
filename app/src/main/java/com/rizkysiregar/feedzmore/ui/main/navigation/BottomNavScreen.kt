package com.rizkysiregar.feedzmore.ui.main.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 21/11/25
 * Description: Software Engineer
 */

sealed class BottomNavScreen(
	val route: String,
	val title: String,
	val icon: ImageVector
) {
	object Feed : BottomNavScreen("feed", "Feed", Icons.Default.Home)
	object Search : BottomNavScreen("search", "Search", Icons.Default.Search)
	object PostFeed : BottomNavScreen("post_feed", "Post Feed", Icons.Default.Add)
	object Profile : BottomNavScreen("profile", "Profile", Icons.Default.Person)
}