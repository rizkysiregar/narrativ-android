package com.rizkysiregar.feedzmore.ui.feature.auth.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.ripple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizkysiregar.feedzmore.R
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme


@Composable
fun LoginScreen(
	username: String,
	password: String,
	onUsernameChange: (String) -> Unit,
	onPasswordChange: (String) -> Unit,
	onLoginClick: () -> Unit,
	onNavigateToRegister: () -> Unit,
	onForgotPasswordClick: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(WindowInsets.systemBars.asPaddingValues())
			.padding(16.dp),
		verticalArrangement = Arrangement.SpaceBetween
	) {
		TopSection(
			username,
			password,
			onUsernameChange,
			onPasswordChange,
			onForgotPasswordClick
		)
		MiddleSection(onLoginClick)
		BottomSection(onNavigateToRegister)
	}

}

@Composable
fun TopSection(
	username: String,
	password: String,
	onUsernameChange: (String) -> Unit,
	onPasswordChange: (String) -> Unit,
	onForgotPasswordClick: () -> Unit
) {
	Column {
		Row(
			modifier = Modifier
				.fillMaxWidth(),
			horizontalArrangement = Arrangement.End
		) {
			Image(
				painter = painterResource(R.drawable.star_black),
				contentDescription = "Header Logo",
				modifier = Modifier
					.size(38.dp),
			)
		}

		Spacer(modifier = Modifier.height(24.dp))

		Text(
			text = "Log In",
			style = MaterialTheme.typography.headlineLarge,
		)

		Spacer(modifier = Modifier.height(24.dp))

		UsernameField(username, onUsernameChange)

		Spacer(modifier = Modifier.height(16.dp))

		PasswordField(password, onPasswordChange)

		Spacer(modifier = Modifier.height(8.dp))

		ForgotPasswordText(onForgotPasswordClick)

	}
}

@Composable
fun MiddleSection(onLoginClick: () -> Unit) {
	Column {
		Button(
			onClick = {
				onLoginClick()
			},
			modifier = Modifier
				.fillMaxWidth()
				.height(52.dp),
			shape = RoundedCornerShape(12.dp),
			colors = ButtonDefaults.buttonColors(
				containerColor = Color.Black,
				contentColor = Color.White
			)
		) {
			Text(
				text = "Log In",
				fontSize = 14.sp
			)
		}

		Spacer(modifier = Modifier.height(24.dp))

		OrLoginWith()

		Spacer(modifier = Modifier.height(16.dp))

		OAuthButtonsRow()
	}
}


@Composable
fun BottomSection(onNavigateToRegister: () -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.Center
	) {
		Text(
			text = "Don't have an account?",
			style = MaterialTheme.typography.bodySmall
		)
		Spacer(modifier = Modifier.width(4.dp))
		Text(
			text = "Sign Up",
			color = Color.Black,
			style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
			modifier = Modifier.clickable(
				interactionSource = remember { MutableInteractionSource() },
				indication = ripple()
			) {
				// navigate to register page
				onNavigateToRegister()
			}
		)
	}
}


// -- TOP SECTION PARTS -- //
// -- //
// -- //
@Composable
fun UsernameField(
	username: String,
	onUsernameChange: (String) -> Unit
) {
	OutlinedTextField(
		value = username,
		onValueChange = onUsernameChange,
		label = { Text("Username") },
		trailingIcon = {
			if (username.isNotEmpty()) {
				Icon(
					imageVector = Icons.Default.Check,
					contentDescription = "Valid Username"
				)
			}
		},
		modifier = Modifier.fillMaxWidth(),
	)
}

@Composable
fun PasswordField(
	password: String,
	onPasswordChange: (String) -> Unit
) {
	var isVisible by remember { mutableStateOf(false) }

	OutlinedTextField(
		value = password,
		onValueChange = onPasswordChange,
		label = { Text("Password") },
		visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
		trailingIcon = {
			IconButton(
				onClick = { isVisible = !isVisible }
			) {
				Icon(
					painter = if (isVisible) painterResource(R.drawable.eye_solid_full) else painterResource(
						R.drawable.eye_slash_solid_full
					),
					contentDescription = "Toggle password visibility",
					modifier = Modifier.size(24.dp)
				)
			}
		},
		modifier = Modifier.fillMaxWidth()
	)
}

@Composable
fun ForgotPasswordText(onForgotPasswordClick: () -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.End
	) {
		TextButton(onClick = onForgotPasswordClick) {
			Text(
				text = "Forgot Password?",
				style = MaterialTheme.typography.bodySmall,
				color = Color.Black
			)
		}
	}
}


// -- Middle SECTION PARTS -- //
// -- //
// -- //
@Composable
fun OrLoginWith() {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier.fillMaxWidth()
	) {
		HorizontalDivider(modifier = Modifier.weight(1f))
		Text(
			text = "Or Login with ",
			style = MaterialTheme.typography.bodySmall
		)
		HorizontalDivider(modifier = Modifier.weight(1f))
	}
}

@Composable
fun OAuthButtonsRow() {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.Center
	) {
		OAuthButton(R.drawable.google_g_logo)
	}
}

@Composable
fun OAuthButton(resId: Int) {
	OutlinedButton(
		onClick = {},
		shape = RoundedCornerShape(12.dp),
		modifier = Modifier.size(56.dp),
		contentPadding = PaddingValues(8.dp)
	) {
		Icon(
			painter = painterResource(resId),
			contentDescription = null
		)
	}
}


@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
	FeedzmoreTheme {
		LoginScreen(
			username = "John Doe",
			password = "",
			onUsernameChange = {},
			onPasswordChange = {},
			onLoginClick = {},
			onNavigateToRegister = {},
			onForgotPasswordClick = {}
		)
	}
}
