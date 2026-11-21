package com.rizkysiregar.narrativ.ui.main.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.rizkysiregar.narrativ.R
import androidx.annotation.StringRes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 21/11/25
 */

sealed class BottomNavScreen(
	val route: String,
	@StringRes val titleRes: Int,
	val icon: IconSource
) {
	object Feed : BottomNavScreen("feed", R.string.tab_feed, IconSource.Vector(Icons.Default.Home))
	object Explore : BottomNavScreen("explore", R.string.tab_explore, IconSource.Vector(Icons.Default.Explore))
	object PostFeed : BottomNavScreen(
		"post_feed",
		R.string.tab_post,
		IconSource.Drawable(R.drawable.square_plus_regular_full)
	)

	object Profile : BottomNavScreen("profile", R.string.tab_profile, IconSource.Vector(Icons.Default.Person))
}

sealed class IconSource {
	data class Vector(val imageVector: ImageVector) : IconSource()
	data class Drawable(@DrawableRes val resId: Int) : IconSource()
}
