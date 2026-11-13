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
	onClose: () -> Unit,
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
			onRemoveMedia = vm::onMediaRemoved,
			onCaptionChange = { vm.onCaptionChanged(it) },
			onClose = onClose,
			onNext = { vm.submitPost() }
		)
	}
}
