package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@Composable
fun FeedScreen() {
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Text("Feed Screen")
	}
}