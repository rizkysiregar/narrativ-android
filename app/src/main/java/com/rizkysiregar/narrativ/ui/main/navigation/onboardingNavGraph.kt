package com.rizkysiregar.narrativ.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.rizkysiregar.narrativ.ui.feature.onboarding.OnboardingRoute
import com.rizkysiregar.narrativ.ui.main.navigation.routes.MainPathRoute

fun NavGraphBuilder.onboardingGraph(navController: NavHostController) {
	composable(MainPathRoute.Onboarding.route) {
		OnboardingRoute(navController)
	}
}
