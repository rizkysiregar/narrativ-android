package com.rizkysiregar.narrativ.ui.feature.auth.reset

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
import androidx.compose.material3.TextButton
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
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.ui.components.OtpInputField
import com.rizkysiregar.narrativ.ui.theme.NarrativTheme
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 02/03/26
 * Description: Software Engineer
 */

@Composable
fun OtpVerificationView(
	onNext: (String) -> Unit,
	onBack: () -> Unit,
	otpSendToEmailAddress: String,
	expirationTimeInMinute: String,
	// when set, the countdown ends with a "send code again" button; [resendAfterSeconds] replaces the OTP lifetime
	// as its length and [countdownKey] restarts it after every send
	onResend: (() -> Unit)? = null,
	resendAfterSeconds: Long? = null,
	countdownKey: Any? = null
) {
	var otpCode by remember { mutableStateOf("") }
	val durationSeconds = resendAfterSeconds
		?: expirationTimeInMinute.toLongOrNull()?.coerceAtLeast(0L)?.times(60L)
		?: 0L
	var remainingSeconds by remember(durationSeconds, countdownKey) { mutableStateOf(durationSeconds) }

	LaunchedEffect(durationSeconds, countdownKey) {
		while (remainingSeconds > 0L) {
			delay(1_000L)
			remainingSeconds--
		}
	}

	Column(
		modifier = Modifier
			.background(color = MaterialTheme.colorScheme.background)
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
					contentDescription = stringResource(R.string.back),
					modifier = Modifier
						.size(24.dp)
						.padding(3.dp)
						.clickable { onBack() }
				)
			}

			Image(
				painter = painterResource(R.drawable.star_black),
				contentDescription = stringResource(R.string.cd_logo),
				modifier = Modifier
					.size(38.dp),
			)

		}

		// --- Header Title and caption --- //
		Spacer(Modifier.height(16.dp))
		Column {
			Text(
				text = stringResource(R.string.otp_check_email),
				style = MaterialTheme.typography.headlineLarge
			)
			Spacer(Modifier.height(8.dp))
			Text(
				text = stringResource(R.string.otp_sent_to, otpSendToEmailAddress),
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
				onNext(otpCode)
			},
			enabled = otpCode.length == 6,
			modifier = Modifier
				.fillMaxWidth()
				.height(52.dp),
			shape = RoundedCornerShape(12.dp),
			colors = ButtonDefaults.buttonColors(
				containerColor = MaterialTheme.colorScheme.primary,
				contentColor = MaterialTheme.colorScheme.onPrimary
			)
		) {
			Text(
				text = stringResource(R.string.verify),
				fontSize = 14.sp
			)
		}

		// resend code or and count down
		Spacer(Modifier.height(16.dp))
		if (onResend != null && remainingSeconds == 0L) {
			TextButton(onClick = onResend) { Text(stringResource(R.string.send_code_again)) }
		} else {
			Text(stringResource(R.string.send_code_again_in, remainingSeconds / 60, remainingSeconds % 60))
		}


	}

}

@Preview
@Composable
private fun OtpVerificationScreenPreview() {
	NarrativTheme {
		OtpVerificationView(
			onNext = {},
			onBack = {},
			otpSendToEmailAddress = "r****y@gmail.com",
			expirationTimeInMinute = "1"
		)
	}
}
