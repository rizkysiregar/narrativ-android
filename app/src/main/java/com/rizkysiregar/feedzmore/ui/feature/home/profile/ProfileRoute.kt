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
	// states from profile viewmodel
	val profileState by profileVm.profileState.collectAsState()
	val userId by profileVm.userId.collectAsState()
	val token by profileVm.token.collectAsState()

	// Get post list state from feed viewmodel
	val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()


	// trigger fetch when entering this page
	LaunchedEffect(userId) {
		if (userId.isNotEmpty()) {
			profileVm.fetchUserProfile()
			feedViewModel.refresh(userId)
		}
	}

	profileState?.let {
		ProfileScreen(
			profileState = it,
			feedState = feedState,
			onRetry = { profileVm.fetchUserProfile() },
			onLoadNextPage = {
				if (userId.isNotEmpty()) {
					feedViewModel.loadNextPage(userId)
				}
			},
			onExitClick = {
				profileVm.clearDataStore()
				onLogoutClick()
			},
			userId = userId,
			token = token
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