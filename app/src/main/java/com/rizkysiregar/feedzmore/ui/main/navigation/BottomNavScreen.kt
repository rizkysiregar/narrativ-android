package com.rizkysiregar.feedzmore.ui.main.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.rizkysiregar.feedzmore.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 21/11/25
 * Description: Software Engineer
 */

sealed class BottomNavScreen(
	val route: String,
	val title: String,
	val icon: IconSource
) {
	object Feed : BottomNavScreen("feed", "Feed", IconSource.Vector(Icons.Default.Home))
	object Explore : BottomNavScreen("explore", "Explore", IconSource.Vector(Icons.Default.Explore))
	object Search : BottomNavScreen("search", "Search", IconSource.Vector(Icons.Default.Search))
	object PostFeed : BottomNavScreen(
		"post_feed",
		"Post Feed",
		IconSource.Drawable(R.drawable.square_plus_regular_full)
	)

	object Profile : BottomNavScreen("profile", "Profile", IconSource.Vector(Icons.Default.Person))
}

sealed class IconSource {
	data class Vector(val imageVector: ImageVector) : IconSource()
	data class Drawable(@DrawableRes val resId: Int) : IconSource()
}
