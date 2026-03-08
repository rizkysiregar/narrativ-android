package com.rizkysiregar.feedzmore.ui.feature.auth.reset

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.ui.base.BaseScreen
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.AuthPathRoutes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 03/03/26
 * Description: Software Engineer
 */

@Composable
fun ResetPasswordRoute(
	navController: NavController, viewModel: ResetPasswordViewModel = hiltViewModel()
) {
	val currentStep by viewModel.currentStep.collectAsState()
	val resetState by viewModel.resetState.collectAsState()
	val context = LocalContext.current

	LaunchedEffect(resetState) {
		if (currentStep == ResetPasswordStep.FINISH && resetState is ResultState.Success) {
			println("## Reset Password Success ##")

			Toast.makeText(
				context,
				"Password reset successful! Please login with you new password!",
				Toast.LENGTH_LONG
			).show()

			navController.navigate(AuthPathRoutes.Login.route) {
				popUpTo(AuthPathRoutes.Login.route) { inclusive = true }
			}
		}
	}

	BaseScreen(
		isLoading = resetState is ResultState.Loading,
		errorMessage = if (resetState is ResultState.Failure) (resetState as ResultState.Failure).message else null,
		onErrorDismiss = { viewModel.resetStateToIdle() }) {
		ResetPasswordScreen(
			currentSte = currentStep,
			resetState = resetState,
			email = viewModel.getSavedEmail(),
			resetToken = viewModel.getSavedResetToken(),
			onSendOtp = { email -> viewModel.sendOtp(email) },
			onVerifyOtp = { request -> viewModel.verifyOtp(request) },
			onResetNewPassword = { request -> viewModel.resetNewPassword(request) },
			onBack = { navController.popBackStack() })
	}

}