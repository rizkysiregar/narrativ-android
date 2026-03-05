package com.rizkysiregar.feedzmore.ui.feature.auth.reset

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.ResetNewPasswordRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.ResetPassVerifyOtpRequest
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 03/03/26
 * Description: Software Engineer
 */

@Composable
fun ResetPasswordScreen(
	currentSte: ResetPasswordStep,
	resetState: ResultState<Any>,
	email: String,
	resetToken: String,
	onSendOtp: (String) -> Unit,
	onVerifyOtp: (ResetPassVerifyOtpRequest) -> Unit,
	onResetNewPassword: (ResetNewPasswordRequest) -> Unit,
	onBack: () -> Unit
) {
	Scaffold { padding ->
		Column(
			modifier = Modifier
				.padding(padding)
				.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			AnimatedContent(
				targetState = currentSte, label = "step_transition"
			) { step ->
				when (step) {
					ResetPasswordStep.EMAIL_ENTRY -> {
						EmailEntryView(
							onNext = onSendOtp,
							onBack = onBack
						)
					}

					ResetPasswordStep.OTP_VERIFICATION -> {
						OtpVerificationView(
							onNext = onVerifyOtp,
							onBack = onBack,
							email = email
						)
					}

					ResetPasswordStep.NEW_PASSWORD_INPUT -> {
						NewPasswordView(
							onNext = onResetNewPassword,
							onBack = onBack,
							email = email,
							resetToken = resetToken
						)
					}

					else -> {}
				}
			}
		}
	}
}


@Preview
@Composable
private fun ResetPasswordScreenPreview() {
	FeedzmoreTheme {
		ResetPasswordScreen(
			currentSte = ResetPasswordStep.EMAIL_ENTRY,
			resetState = ResultState.Idle,
			email = "",
			resetToken = "",
			onSendOtp = {},
			onVerifyOtp = {},
			onResetNewPassword = {},
			onBack = {}
		)
	}
}