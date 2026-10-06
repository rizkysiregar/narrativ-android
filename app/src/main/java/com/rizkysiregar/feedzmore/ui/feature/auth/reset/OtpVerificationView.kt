package com.rizkysiregar.feedzmore.ui.feature.auth.reset

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizkysiregar.feedzmore.R
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.ResetPassVerifyOtpRequest
import com.rizkysiregar.feedzmore.ui.components.OtpInputField
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme
import kotlinx.coroutines.delay

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 02/03/26
 * Description: Software Engineer
 */

@Composable
fun OtpVerificationView(
	onNext: (ResetPassVerifyOtpRequest) -> Unit,
	onBack: () -> Unit,
	email: String,
	otpSendToEmailAddress: String,
	expirationTimeInMinute: String
) {
	var otpCode by remember { mutableStateOf("") }
	val durationSeconds = expirationTimeInMinute.toLongOrNull()?.coerceAtLeast(0L)?.times(60L) ?: 0L
	var remainingSeconds by remember(durationSeconds) { mutableStateOf(durationSeconds) }

	LaunchedEffect(durationSeconds) {
		while (remainingSeconds > 0L) {
			delay(1_000L)
			remainingSeconds--
		}
	}

	Column(
		modifier = Modifier
			.background(color = Color.White)
			.fillMaxSize()
			.padding(16.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {

		// --- Header: Back Button & Logo --- //
		Row(
			modifier = Modifier
				.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Box(
				modifier = Modifier
					.padding(4.dp)
					.border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
					.size(35.dp),
				contentAlignment = Alignment.Center
			) {
				Icon(
					painter = painterResource(R.drawable.outline_arrow_back_ios_24),
					contentDescription = "Back",
					modifier = Modifier
						.size(24.dp)
						.padding(3.dp)
						.clickable { onBack() }
				)
			}

			Image(
				painter = painterResource(R.drawable.star_black),
				contentDescription = "Header Logo",
				modifier = Modifier
					.size(38.dp),
			)

		}

		// --- Header Title and caption --- //
		Spacer(Modifier.height(16.dp))
		Column {
			Text(
				text = "Please check your email",
				style = MaterialTheme.typography.headlineLarge
			)
			Spacer(Modifier.height(8.dp))
			Text(
				text = "We’ve sent a code to $otpSendToEmailAddress",
				style = MaterialTheme.typography.bodyMedium
			)
		}


		// --- OTP Input Field --- //
		Spacer(Modifier.height(32.dp))
		OtpInputField(
			otpText = otpCode,
			onOtpTextChange = { otpCode = it.filter(Char::isDigit).take(6) },
		)

		// --- Button Verify --- //
		Spacer(Modifier.height(24.dp))
		Button(
			onClick = {
				onNext(
					ResetPassVerifyOtpRequest(
						email = email,
						otp = otpCode
					)
				)
			},
			enabled = otpCode.length == 6,
			modifier = Modifier
				.fillMaxWidth()
				.height(52.dp),
			shape = RoundedCornerShape(12.dp),
			colors = ButtonDefaults.buttonColors(
				containerColor = Color.Black,
				contentColor = Color.White
			)
		) {
			Text(
				text = "Verify",
				fontSize = 14.sp
			)
		}

		// resend code or and count down
		Spacer(Modifier.height(16.dp))
		Text("Send code again in %02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60))


	}

}

@Preview
@Composable
private fun OtpVerificationScreenPreview() {
	FeedzmoreTheme {
		OtpVerificationView(
			onNext = {},
			onBack = {},
			email = "rizky@gmail.com",
			otpSendToEmailAddress = "r****y@gmail.com",
			expirationTimeInMinute = "1"
		)
	}
}
