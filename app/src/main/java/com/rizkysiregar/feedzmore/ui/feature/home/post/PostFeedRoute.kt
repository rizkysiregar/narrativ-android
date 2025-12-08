package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController

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

	PostFeedScreen(
		uiState = uiState,
		onPickImage = { vm.onImageSelected(it) },
		onCaptionChange = { vm.onCaptionChanged(it) },
		onNext = {
			Log.i("onNext Button", vm.uiState.value.toString())
		}
	)


}