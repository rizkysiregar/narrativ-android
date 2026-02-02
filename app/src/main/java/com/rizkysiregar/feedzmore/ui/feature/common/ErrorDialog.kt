package com.rizkysiregar.feedzmore.ui.feature.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.rizkysiregar.feedzmore.R
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 16/11/25
 * Description: Software Engineer
 */


@Composable
fun ErrorDialog(
	message: String,
	onDismiss: () -> Unit
) {
	Dialog(
		onDismissRequest = onDismiss
	) {
		Surface(
			shape = RoundedCornerShape(20.dp),
			tonalElevation = 6.dp,
			color = MaterialTheme.colorScheme.surface
		) {
			Column(
				modifier = Modifier
					.padding(20.dp)
					.widthIn(min = 260.dp, max = 320.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				// Lottie Animation
				val composition by rememberLottieComposition(
					LottieCompositionSpec.RawRes(R.raw.error_animation)
				)

				LottieAnimation(
					composition = composition,
					iterations = 3,
					modifier = Modifier.size(150.dp)
				)
				Spacer(modifier = Modifier.height(12.dp))

				Text(
					text = "Oops!",
					style = MaterialTheme.typography.titleLarge,
					fontWeight = FontWeight.Bold
				)
				Spacer(modifier = Modifier.height(8.dp))

				Text(
					text = message,
					style = MaterialTheme.typography.bodyMedium,
					textAlign = TextAlign.Center,
					modifier = Modifier.padding(horizontal = 8.dp)
				)
				Spacer(modifier = Modifier.height(20.dp))

				Button(
					onClick = onDismiss,
					modifier = Modifier.fillMaxWidth(),
					shape = RoundedCornerShape(12.dp)
				) {
					Text("Dismiss")
				}
			}
		}
	}
}

@Composable
@Preview(showBackground = true)
fun ErrorDialogPreview() {
	FeedzmoreTheme {
		ErrorDialog(
			message = "Something went wrong",
			onDismiss = {}
		)
	}
}