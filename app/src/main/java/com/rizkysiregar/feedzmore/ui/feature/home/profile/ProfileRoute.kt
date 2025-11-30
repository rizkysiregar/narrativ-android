package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreandroidnativeTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@Composable
fun ProfileRoute(
	vm: ProfileViewModel = hiltViewModel(),
	onLogoutClick: () -> Unit
) {
	ProfileScreen(
		onExitClick = {
			vm.clearDataStore()
			onLogoutClick()
		}
	)
}


@Preview(showBackground = true)
@Composable
private fun ProfileRoutePreview() {
	FeedzmoreandroidnativeTheme {
		ProfileRoute(
			onLogoutClick = {}
		)
	}
}