package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreandroidnativeTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
	onExitClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Profile") },
				actions = {
					IconButton(onClick = onExitClick) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ExitToApp,
							contentDescription = "Exit App"
						)
					}
				},
				windowInsets = WindowInsets(0)
			)
		},
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(
					top = innerPadding.calculateTopPadding(),
					bottom = innerPadding.calculateBottomPadding(),
				)
		) {

		}
	}
}


@Preview(showSystemUi = true)
@Composable
private fun ProfileScreenPreview() {
	FeedzmoreandroidnativeTheme {
		ProfileScreen(
			onExitClick = {},
			modifier = Modifier
		)
	}
}