package com.rizkysiregar.feedzmore.ui.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.ui.base.BaseScreen

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/25
 * Description: Software Engineer
 */


@Composable
fun AuthRoute(
	navController: NavController,
	viewModel: AuthViewModel = hiltViewModel(),
	onLoginSuccess: () -> Unit
) {
	// Context
	val context = LocalContext.current

	// username and password input
	var username by remember { mutableStateOf("") }
	var password by remember { mutableStateOf("") }

	// view model
	val loginState by viewModel.loginState.collectAsState()

	LaunchedEffect(loginState) {
		if (loginState is ResultState.Success) {
			onLoginSuccess()
			viewModel.resetState()
		}
	}

	BaseScreen(
		isLoading = loginState is ResultState.Loading,
		errorMessage = if (loginState is ResultState.Failure) (loginState as ResultState.Failure).message else null,
		onErrorDismiss = { viewModel.resetState() }
	) {
		AuthScreen(
			username = username,
			password = password,
			onUsernameChange = { username = it },
			onPasswordChange = { password = it },
			onLoginClick = {
				viewModel.login(username, password)
			}
		)
	}


//	AuthScreen(
//		username = username,
//		password = password,
//		loginState = loginState,
//		onUsernameChange = { username = it },
//		onPasswordChange = { password = it },
//		onLoginClick = { viewModel.login(username, password) },
//		onDismiss = { viewModel.clearError() },
//		onGoogleClick = {
//			println("## clicked")
//			viewModel.buildLoginGoogleIntent()
//		}
//	)

}

