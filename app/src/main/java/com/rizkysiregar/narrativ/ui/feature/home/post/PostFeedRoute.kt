package com.rizkysiregar.narrativ.ui.feature.home.post

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rizkysiregar.narrativ.ui.base.BaseScreen
import com.rizkysiregar.narrativ.ui.components.rememberReduceMotion
import kotlinx.coroutines.delay

// How long the composer takes to fold up before the plane takes over.
private const val FOLD_MILLIS = 320

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 06/12/25
 */


@Composable
fun PostFeedRoute(
	/** Called when the post is queued; [origin] is where the plane should take off, in root coordinates. */
	onPostSuccess: (origin: Offset) -> Unit,
	onClose: () -> Unit,
	vm: PostFeedViewModel = hiltViewModel()
) {
	val uiState by vm.uiState.collectAsState()
	val isPreparing by vm.isPreparing.collectAsState()

	val reduceMotion = rememberReduceMotion()
	var isLaunching by remember { mutableStateOf(false) }
	var bodyCenter by remember { mutableStateOf(Offset.Zero) }
	val launchProgress by animateFloatAsState(
		targetValue = if (isLaunching) 1f else 0f,
		animationSpec = tween(FOLD_MILLIS),
		label = "post-fold"
	)

	LaunchedEffect(Unit) {
		vm.navigateHome.collect {
			if (!reduceMotion) {
				isLaunching = true
				delay(FOLD_MILLIS.toLong())
			}
			onPostSuccess(bodyCenter)
		}
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
			onNext = { vm.submitPost() },
			isLaunching = isLaunching,
			launchProgress = launchProgress,
			onBodyPositioned = { bodyCenter = it }
		)
	}
}
