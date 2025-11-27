package com.rizkysiregar.feedzmore.ui.feature.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreandroidnativeTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */


@Composable
fun SplashScreen() {
	// UI
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Text("Loading ..")
	}
}


@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
	FeedzmoreandroidnativeTheme {
		SplashScreen()
	}
}

