package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.PostingFeedRequest
import com.rizkysiregar.feedzmore.ui.feature.common.BaseScreen
import com.rizkysiregar.feedzmore.ui.main.navigation.BottomNavScreen

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 06/12/25
 * Description: Software Engineer
 */


@Composable
fun PostFeedRoute(
	navController: NavHostController,
	vm: PostFeedViewModel = hiltViewModel()
) {
	val uiState by vm.uiState.collectAsState()
	val postingState by vm.postingState.collectAsState()

	LaunchedEffect(postingState) {
		if (postingState is ResultState.Success) {
			// navigate to home screen
			navController.navigate(BottomNavScreen.Feed.route) {
				popUpTo(BottomNavScreen.PostFeed.route) {
					inclusive = true
				}
				launchSingleTop = true
			}
			vm.resetPostingState()
		}
	}

	BaseScreen(
		isLoading = postingState is ResultState.Loading,
		errorMessage = if (postingState is ResultState.Failure) (postingState as ResultState.Failure).message else null,
		onErrorDismiss = { vm.resetPostingState() }) {
		PostFeedScreen(
			uiState = uiState,
			onPickImage = { vm.onImageSelected(it) },
			onCaptionChange = { vm.onCaptionChanged(it) },
			onNext = {
				Log.i("onNext Button", vm.uiState.value.toString())
				vm.postingFeed(
					PostingFeedRequest(
						imageUri = uiState.selectedImage!!,
						caption = uiState.caption,
						imagePart = null,
						username = ""
					)
				)
			})
	}
}