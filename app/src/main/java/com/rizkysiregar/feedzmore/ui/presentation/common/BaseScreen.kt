package com.rizkysiregar.feedzmore.ui.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/25
 * Description: Software Engineer
 */


@Composable
fun BaseScreen(
	isLoading: Boolean = false,
	errorMessage: String? = null,
	topBar: @Composable (() -> Unit)? = null,
	onErrorDismiss: () -> Unit = {},
	content: @Composable () -> Unit
) {
	Scaffold(
		topBar = {
			topBar?.invoke()
		}
	) { padding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
		) {
			// actual page content
			content()

			// error message
			if (errorMessage != null) {
				ErrorDialog(
					message = errorMessage,
					onDismiss = onErrorDismiss
				)
			}

			// Loading Overlay
			if (isLoading) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(Color.Black.copy(alpha = 0.5f))
						.pointerInput(Unit) {},
					contentAlignment = Alignment.Center
				) {
					CircularProgressIndicator()
				}
			}
		}
	}
}





































