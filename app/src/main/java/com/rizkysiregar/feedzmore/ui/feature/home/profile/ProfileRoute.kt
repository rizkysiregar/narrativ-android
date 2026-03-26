package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedViewModel
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@Composable
fun ProfileRoute(
	profileVm: ProfileViewModel = hiltViewModel(),
	feedViewModel: FeedViewModel = hiltViewModel(),
	onLogoutClick: () -> Unit
) {
	val profileState by profileVm.profileState.collectAsState()
	val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()
	val username by profileVm.usernameDataStore.collectAsState()

	// trigger fetch when entering this page
	LaunchedEffect(username) {
		profileVm.fetchUserProfile()
		if (username.isNotBlank()) {
			print("## profile username: $username")
			feedViewModel.setUsername(username)
			feedViewModel.refresh()
		}
	}

	profileState?.let {
		ProfileScreen(
			profileState = it,
			feedState = feedState,
			onRetry = { profileVm.fetchUserProfile() },
			onLoadNextPage = {
				feedViewModel.loadNextPage()
			},
			onExitClick = {
				profileVm.clearDataStore()
				onLogoutClick()
			},
		)
	}
}


@Preview(showBackground = true)
@Composable
private fun ProfileRoutePreview() {
	FeedzmoreTheme {
		ProfileRoute(
			onLogoutClick = {}
		)
	}
}