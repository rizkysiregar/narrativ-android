package com.rizkysiregar.narrativ.ui.feature.auth.regiter

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.ui.base.BaseScreen
import com.rizkysiregar.narrativ.ui.feature.auth.reset.OtpVerificationView
import com.rizkysiregar.narrativ.ui.main.navigation.routes.MainPathRoute
import com.rizkysiregar.narrativ.ui.main.navigation.routes.AuthPathRoutes
import com.rizkysiregar.narrativ.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 24/02/26
 * Description: Software Engineer
 */


@Composable
fun RegisterRoute(
	navController: NavController,
	registerViewModel: RegisterViewModel = hiltViewModel(),
	onRegisterSuccess: () -> Unit
) {
	val context = LocalContext.current
	val resources = LocalResources.current

	val step by registerViewModel.step.collectAsState()
	val registerState by registerViewModel.state.collectAsState()
	val otpDelivery by registerViewModel.otpDelivery.collectAsState()

	LaunchedEffect(registerState) {
		if (registerState is ResultState.Success) {
			println("## register success")
			Toast.makeText(
				context, resources.getString(R.string.registration_success), Toast.LENGTH_SHORT
			).show()

			onRegisterSuccess()
			registerViewModel.resetState()
		}
	}

	BackHandler(enabled = step == RegisterStep.OTP_VERIFICATION) {
		registerViewModel.backToForm()
	}

	BaseScreen(
		isLoading = registerState is ResultState.Loading,
		errorMessage = (registerState as? ResultState.Failure)?.message,
		onErrorDismiss = { registerViewModel.resetState() }) {
		when (step) {
			RegisterStep.FORM -> RegisterScreen(
				onRegisterClick = { finalState -> registerViewModel.submit(finalState) },
				onNavigateToLogin = {
					navController.navigate(AuthPathRoutes.Login.route) {
						popUpTo(AuthPathRoutes.Register.route) { inclusive = true }
						launchSingleTop = true
					}
				},
				onLegalClick = { document -> navController.navigate(MainPathRoute.Legal.create(document.path)) },
				initialState = registerViewModel.form
			)

			RegisterStep.OTP_VERIFICATION -> OtpVerificationView(
				onNext = { otp -> registerViewModel.verifyOtp(otp) },
				onBack = { registerViewModel.backToForm() },
				otpSendToEmailAddress = otpDelivery?.otpSendToEmailAddress.orEmpty(),
				expirationTimeInMinute = otpDelivery?.expirationTimeInMinute.orEmpty(),
				onResend = { registerViewModel.resendOtp() },
				resendAfterSeconds = RESEND_COOLDOWN_SECONDS,
				countdownKey = otpDelivery?.sentCount
			)
		}
	}
}

// the server refuses a new registration OTP for this long after the last one
private const val RESEND_COOLDOWN_SECONDS = 60L
