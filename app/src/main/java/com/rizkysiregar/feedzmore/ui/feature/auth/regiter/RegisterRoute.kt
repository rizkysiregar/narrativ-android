package com.rizkysiregar.feedzmore.ui.feature.auth.regiter

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.RegisterRequest
import com.rizkysiregar.feedzmore.ui.base.BaseScreen
import com.rizkysiregar.feedzmore.ui.feature.auth.AuthViewModel
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.AuthPathRoutes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 24/02/26
 * Description: Software Engineer
 */


@Composable
fun RegisterRoute(
	navController: NavController,
	authViewModel: AuthViewModel = hiltViewModel(),
	onRegisterSuccess: () -> Unit
) {
	val context = LocalContext.current

	// listen of the registration from viewmodel
	val registerState by authViewModel.registerState.collectAsState()

	LaunchedEffect(registerState) {
		if (registerState is ResultState.Success) {
			println("## register success")
			Toast.makeText(
				context, "Registration successful! Please log in.", Toast.LENGTH_SHORT
			).show()

			onRegisterSuccess()
			authViewModel.resetRegisterState()
		}
	}

	BaseScreen(
		isLoading = registerState is ResultState.Loading,
		errorMessage = if (registerState is ResultState.Failure) (registerState as ResultState.Failure).message else null,
		onErrorDismiss = { authViewModel.resetRegisterState() }) {
		RegisterScreen(
			onRegisterClick = { finalState ->
				println("finalState register: $finalState")
				val request = RegisterRequest(
					username = finalState.username,
					password = finalState.password,
					email = finalState.email,
					firstName = finalState.firstName,
					lastName = finalState.lastName
				)

				authViewModel.register(request)
			},
			onNavigateToLogin = {
				navController.navigate(AuthPathRoutes.Login.route) {
					popUpTo(AuthPathRoutes.Register.route) { inclusive = true }
					launchSingleTop = true
				}
			}
		)
	}
}
