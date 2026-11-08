package com.rizkysiregar.feedzmore.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.ui.feature.common.ErrorDialog

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
		topBar = { topBar?.invoke() }
	) { padding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(
					top = if (topBar != null) padding.calculateTopPadding() else 0.dp,
					bottom = padding.calculateBottomPadding()
				)
		) {

			// always visible screen UI
			content()

			if (errorMessage != null) {
				ErrorDialog(
					message = errorMessage,
					onDismiss = onErrorDismiss
				)
			}

			if (isLoading) {
				Box(
					Modifier
						.fillMaxSize()
						.background(MaterialTheme.colorScheme.scrim),
					contentAlignment = Alignment.Center
				) {
					CircularProgressIndicator()
				}
			}
		}
	}
}





































