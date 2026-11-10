package com.rizkysiregar.feedzmore.ui.feature.home.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
	val isPreparing by vm.isPreparing.collectAsState()

	LaunchedEffect(Unit) {
		vm.navigateHome.collect { onPostSuccess() }
	}

	BaseScreen(
		errorMessage = uiState.validationError,
		onErrorDismiss = { vm.clearValidationError() }
	) {
		PostFeedScreen(
			uiState = uiState,
			isPreparing = isPreparing,
			onPickMedia = vm::onMediaPicked,
			onToggleRecentImage = vm::onRecentImageToggled,
			onRemoveMedia = vm::onMediaRemoved,
			onPreviewSelected = vm::onPreviewSelected,
			onLoadRecentImages = vm::loadRecentImages,
			onCaptionChange = { vm.onCaptionChanged(it) },
			onNext = { vm.submitPost() }
		)
	}
}
