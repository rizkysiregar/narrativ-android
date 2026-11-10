package com.rizkysiregar.feedzmore.ui.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.rizkysiregar.feedzmore.ui.main.navigation.BottomNavScreen
import com.rizkysiregar.feedzmore.ui.main.navigation.IconSource
import com.rizkysiregar.feedzmore.ui.theme.Accent300
import com.rizkysiregar.feedzmore.ui.theme.Accent500
import com.rizkysiregar.feedzmore.ui.theme.Accent700
import com.rizkysiregar.feedzmore.ui.theme.AccentSubtle
import com.rizkysiregar.feedzmore.ui.theme.BorderDefault
import com.rizkysiregar.feedzmore.ui.theme.Like
import com.rizkysiregar.feedzmore.ui.theme.OnAccent
import com.rizkysiregar.feedzmore.ui.theme.Surface1
import com.rizkysiregar.feedzmore.ui.theme.TextMuted

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 21/11/25
 * Description: Software Engineer
 */

private val BarHeight = 52.dp
private val CenterButtonSize = 46.dp
private val CenterButtonLift = 14.dp

/**
 * Compact floating pill bottom bar: the selected tab gets an accent capsule behind its icon,
 * and the post action is a raised gradient button in the middle.
 */
@Composable
fun BottomBar(
	navController: NavController
) {
	val leftItems = listOf(BottomNavScreen.Feed, BottomNavScreen.Explore)
	val rightItems = listOf(BottomNavScreen.Search, BottomNavScreen.Profile)
	val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

	fun navigateTo(item: BottomNavScreen) {
		navController.navigate(item.route) {
			launchSingleTop = true
			restoreState = true
			popUpTo(navController.graph.startDestinationId) {
				saveState = true
			}
		}
	}

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.navigationBarsPadding()
			.padding(start = 24.dp, end = 24.dp, top = CenterButtonLift, bottom = 8.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.height(BarHeight)
				.shadow(
					elevation = 12.dp,
					shape = RoundedCornerShape(26.dp),
					ambientColor = Accent500,
					spotColor = Accent500
				)
				.background(Surface1.copy(alpha = 0.96f), RoundedCornerShape(26.dp))
				.border(BorderStroke(1.dp, BorderDefault), RoundedCornerShape(26.dp))
				.padding(horizontal = 6.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			leftItems.forEach { item ->
				BottomBarSlot(
					item = item,
					selected = currentRoute == item.route,
					onClick = { navigateTo(item) }
				)
			}
			Box(modifier = Modifier.width(CenterButtonSize + 8.dp))
			rightItems.forEach { item ->
				BottomBarSlot(
					item = item,
					selected = currentRoute == item.route,
					onClick = { navigateTo(item) }
				)
			}
		}

		CenterPostButton(
			selected = currentRoute == BottomNavScreen.PostFeed.route,
			onClick = { navigateTo(BottomNavScreen.PostFeed) },
			modifier = Modifier
				.align(Alignment.TopCenter)
				.offset(y = -CenterButtonLift)
		)
	}
}

@Composable
private fun RowScope.BottomBarSlot(
	item: BottomNavScreen,
	selected: Boolean,
	onClick: () -> Unit
) {
	// Equal slots keep icons evenly spaced.
	Box(
		modifier = Modifier
			.weight(1f)
			.fillMaxHeight(),
		contentAlignment = Alignment.Center
	) {
		BottomBarTab(item = item, selected = selected, onClick = onClick)
	}
}

@Composable
private fun BottomBarTab(
	item: BottomNavScreen,
	selected: Boolean,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	val background by animateColorAsState(
		targetValue = if (selected) AccentSubtle else Color.Transparent,
		label = "tabBackground"
	)
	val tint by animateColorAsState(
		targetValue = if (selected) Accent300 else TextMuted,
		label = "tabTint"
	)

	Row(
		modifier = modifier
			.size(width = 46.dp, height = 36.dp)
			.background(background, CircleShape)
			.clickable(
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
				onClick = onClick
			)
			,
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.Center
	) {
		when (item.icon) {
			is IconSource.Vector -> Icon(
				imageVector = item.icon.imageVector,
				contentDescription = item.title,
				tint = tint,
				modifier = Modifier.size(22.dp)
			)

			is IconSource.Drawable -> Icon(
				painter = painterResource(item.icon.resId),
				contentDescription = item.title,
				tint = tint,
				modifier = Modifier.size(22.dp)
			)
		}
	}
}

@Composable
private fun CenterPostButton(
	selected: Boolean,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier
			.size(CenterButtonSize)
			.shadow(
				elevation = if (selected) 18.dp else 12.dp,
				shape = CircleShape,
				ambientColor = Accent500,
				spotColor = Like
			)
			.background(
				Brush.linearGradient(listOf(Accent300, Accent500, Accent700)),
				CircleShape
			)
			.border(2.dp, Surface1, CircleShape)
			.clickable(
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
				onClick = onClick
			),
		contentAlignment = Alignment.Center
	) {
		Icon(
			imageVector = Icons.Default.Add,
			contentDescription = BottomNavScreen.PostFeed.title,
			tint = OnAccent,
			modifier = Modifier.size(26.dp)
		)
	}
}
