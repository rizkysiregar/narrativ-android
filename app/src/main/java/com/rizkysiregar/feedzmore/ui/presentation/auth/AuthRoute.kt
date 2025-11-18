package com.rizkysiregar.feedzmore.ui.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/25
 * Description: Software Engineer
 */


@Composable
fun AuthRoute(
	viewModel: AuthViewModel = hiltViewModel()
) {

	// Context
	val context = LocalContext.current


	// username and password input
	var username by remember { mutableStateOf("") }
	var password by remember { mutableStateOf("") }

	// view model
	val loginState by viewModel.loginState.collectAsState()

	// Google OAuth2 Launcher


//	val googleOAuth2Launcher = rememberLauncherForActivityResult(
//		contract = ActivityResultContracts.StartActivityForResult()
//	) { result ->
//		val data = result.data
//		val token = data?.getStringExtra("token")
//		println("## TOKEN RESULT: $token")
//	}

	AuthScreen(
		username = username,
		password = password,
		loginState = loginState,
		onUsernameChange = { username = it },
		onPasswordChange = { password = it },
		onLoginClick = { viewModel.login(username, password) },
		onDismiss = { viewModel.clearError() },
		onGoogleClick = {
			println("## clicked")
			viewModel.buildLoginGoogleIntent()
		}
	)

}

