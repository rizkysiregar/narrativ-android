package com.rizkysiregar.feedzmore.ui.feature.auth

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.R
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreandroidnativeTheme


@Composable
fun LoginScreen(
	username: String,
	password: String,
	onUsernameChange: (String) -> Unit,
	onPasswordChange: (String) -> Unit,
	onLoginClick: () -> Unit,

	) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp),
		verticalArrangement = Arrangement.Center
	) {
		OutlinedTextField(
			value = username,
			onValueChange = onUsernameChange,
			label = { Text("Username") },
			modifier = Modifier.fillMaxWidth()
		)
		Spacer(modifier = Modifier.height(8.dp))

		OutlinedTextField(
			value = password,
			onValueChange = onPasswordChange,
			label = { Text("Password") },
			modifier = Modifier.fillMaxWidth()
		)
		Spacer(modifier = Modifier.height(16.dp))

		Button(
			onClick = onLoginClick,
			modifier = Modifier.fillMaxWidth()
		) {
			Text("Login")
		}
		Spacer(modifier = Modifier.height(16.dp))

	}
}


@Composable
fun OAuthIconRow(
	onGoogleClick: () -> Unit,
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.Center
	) {
		OAuthIcon(
			icon = R.drawable.google_g_logo,
			contentDescription = "Google",
			onClick = onGoogleClick
		)
	}
}

@Composable
fun OAuthIcon(
	icon: Int,
	contentDescription: String,
	onClick: () -> Unit
) {
	Box(
		modifier = Modifier
			.size(56.dp)
			.clip(RoundedCornerShape(16.dp))
			.background(MaterialTheme.colorScheme.surface)
			.border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
			.clickable { onClick() },
		contentAlignment = Alignment.Center
	) {
		Icon(
			painter = painterResource(icon),
			contentDescription = contentDescription,
			tint = Color.Unspecified,
			modifier = Modifier.size(28.dp)
		)
	}
}


@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
	FeedzmoreandroidnativeTheme {
		LoginScreen(
			username = "John Doe",
			password = "",
			onUsernameChange = {},
			onPasswordChange = {},
			onLoginClick = {},
		)
	}
}