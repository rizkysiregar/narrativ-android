package com.rizkysiregar.feedzmore.ui.feature.auth.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.ui.base.BaseScreen
import com.rizkysiregar.feedzmore.ui.feature.auth.AuthViewModel
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.AuthPathRoutes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/25
 * Description: Software Engineer
 */


@Composable
fun LoginRoute(
	navController: NavController,
	viewModel: AuthViewModel = hiltViewModel(),
	onLoginSuccess: () -> Unit
) {
	// username and password input
	var username by remember { mutableStateOf("") }
	var password by remember { mutableStateOf("") }

	// view model
	val loginState by viewModel.loginState.collectAsState()
	val googleLoginState by viewModel.googleLoginState.collectAsState()

	// handle regular login
	LaunchedEffect(loginState) {
		if (loginState is ResultState.Success) {
			onLoginSuccess()
			viewModel.resetLoginState()
		}
	}

	// handle Google login success
	LaunchedEffect(googleLoginState) {
		if (googleLoginState is ResultState.Success) {
			onLoginSuccess()
			viewModel.resetGoogleLoginState()
		}
	}

	val isLoading = loginState is ResultState.Loading || googleLoginState is ResultState.Loading
	val errorMessage = when {
		loginState is ResultState.Failure -> (loginState as ResultState.Failure).message
		googleLoginState is ResultState.Failure -> (googleLoginState as ResultState.Failure).message
		else -> null
	}

	BaseScreen(
		isLoading,
		errorMessage,
		onErrorDismiss = {
			viewModel.resetLoginState()
			viewModel.resetGoogleLoginState()
		}
	) {
		LoginScreen(
			username = username,
			password = password,
			onUsernameChange = { username = it },
			onPasswordChange = { password = it },
			onLoginClick = {
				viewModel.login(username, password)
			},
			onNavigateToRegister = {
				navController.navigate(AuthPathRoutes.Register.route)
			},
			onForgotPasswordClick = {
				navController.navigate(AuthPathRoutes.ResetPassword.route)
			},
			onGoogleLoginClick = {
				println("google login")
				viewModel.signInWithGoogle()
			}
		)
	}
}




