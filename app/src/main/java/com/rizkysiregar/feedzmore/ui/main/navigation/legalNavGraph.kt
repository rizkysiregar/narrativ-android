package com.rizkysiregar.feedzmore.ui.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rizkysiregar.feedzmore.ui.feature.legal.LegalDocument
import com.rizkysiregar.feedzmore.ui.feature.legal.LegalScreen
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.MainPathRoute

fun NavGraphBuilder.legalGraph(navController: NavHostController) {
	composable(
		route = MainPathRoute.Legal.route,
		arguments = listOf(navArgument(MainPathRoute.Legal.ARG_DOC) { type = NavType.StringType })
	) { entry ->
		// an unknown document name just closes the screen
		val document = LegalDocument.fromPath(entry.arguments?.getString(MainPathRoute.Legal.ARG_DOC))
		if (document == null) {
			navController.popBackStack()
		} else {
			LegalScreen(document = document, onBackClick = { navController.popBackStack() })
		}
	}
}
