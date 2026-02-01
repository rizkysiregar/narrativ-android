package com.rizkysiregar.feedzmore.ui.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizkysiregar.feedzmore.R
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
		modifier = Modifier
			.fillMaxSize()
			.background(Color.LightGray)
		,
		contentAlignment = Alignment.Center // This centers the Column vertically and horizontally
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally // This centers the Text and LottieAnimation within the Column
		) {
//			Image(
//				painter = painterResource(R.drawable.feedzmore_logo_title_no_bg_real),
//				contentDescription = "Feedzmore Logo",
//				modifier = Modifier
//					.background(Color.Transparent)
//
//			)

			Text("Loading .. ")
		}
	}

}


@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
	FeedzmoreandroidnativeTheme {
		SplashScreen()
	}
}

