package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
	val profileState by vm.profileState.collectAsState()
	val token by vm.token.collectAsState()
	val userId by vm.userId.collectAsState()

	// trigger fetch when entering this page
	LaunchedEffect(Unit) {
		vm.fetchUserProfile()
	}

	profileState?.let {
		ProfileScreen(
			profileState = it,
			onRetry = { vm.fetchUserProfile() },
			onExitClick = {
				vm.clearDataStore()
				onLogoutClick()
			},
			userId = userId
		)
	}
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