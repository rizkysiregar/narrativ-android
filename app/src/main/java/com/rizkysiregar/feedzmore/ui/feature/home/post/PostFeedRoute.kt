package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.PostingFeedRequest
import com.rizkysiregar.feedzmore.ui.base.BaseScreen

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 06/12/25
 * Description: Software Engineer
 */


@Composable
fun PostFeedRoute(
	onPostSuccess: () -> Unit,
	vm: PostFeedViewModel = hiltViewModel()
) {
	val uiState by vm.uiState.collectAsState()
	val postingState by vm.postingState.collectAsState()

	LaunchedEffect(postingState) {
		if (postingState is ResultState.Success) {
			onPostSuccess()
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
			onLoadRecentImages = vm::loadRecentImages,
			onCaptionChange = { vm.onCaptionChanged(it) },
			onNext = {
				Log.i("onNext Button", vm.uiState.value.toString())
				vm.postingFeed(
					PostingFeedRequest(
					mediaUri = uiState.selectedImage!!,
					caption = uiState.caption,
					mediaPart = null,
					username = ""
					)
				)
			})
	}
}
