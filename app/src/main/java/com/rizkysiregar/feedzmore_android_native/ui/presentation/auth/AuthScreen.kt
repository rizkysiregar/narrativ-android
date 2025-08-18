package com.rizkysiregar.feedzmore_android_native.ui.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rizkysiregar.feedzmore_android_native.data.remote.model.LoginResponse
import com.rizkysiregar.feedzmore_android_native.data.remote.model.ResultState

@Composable
fun AuthScreen(
	viewModel: AuthViewModel = hiltViewModel(),
) {
	var username by remember { mutableStateOf("") }
	var password by remember { mutableStateOf("") }

	val loginState by viewModel.loginState.collectAsState()

	Column(
		modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
		verticalArrangement = Arrangement.Center
	) {
		OutlinedTextField(
			value = username,
			onValueChange = { username = it },
			label = { Text("Username") },
			modifier = Modifier.fillMaxWidth()
		)

		Spacer(modifier = Modifier.height(8.dp))

		OutlinedTextField(
			value = password,
			onValueChange = { password = it },
			label = { Text("Password") },
			modifier = Modifier.fillMaxWidth()
		)

		Spacer(modifier = Modifier.height(16.dp))

		Button(
			onClick = { viewModel.login(username, password) },
			modifier = Modifier.fillMaxWidth()
		) {
			Text("Login")
		}

		Spacer(modifier = Modifier.height(16.dp))


		when (loginState) {
			is ResultState.Loading -> {
				CircularProgressIndicator()
			}

			is ResultState.Success -> {
				val data = (loginState as ResultState.Success<LoginResponse>).data
				Text("Welcome ${data.username}")
			}

			is ResultState.Failure -> {
				val error = (loginState as ResultState.Failure).message
				Text("Login failed: $error")
			}

			else -> {

			}
		}
	}

}