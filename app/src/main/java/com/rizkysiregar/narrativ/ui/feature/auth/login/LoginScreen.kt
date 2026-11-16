package com.rizkysiregar.narrativ.ui.feature.auth.login

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.ui.theme.NarrativTheme
import androidx.compose.ui.res.stringResource


@Composable
fun LoginScreen(
	username: String,
	password: String,
	usernameError: String?,
	passwordError: String?,
	onUsernameChange: (String) -> Unit,
	onPasswordChange: (String) -> Unit,
	onLoginClick: () -> Unit,
	onGoogleLoginClick: () -> Unit,
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
			usernameError,
			passwordError,
			onUsernameChange,
			onPasswordChange,
			onForgotPasswordClick
		)
		MiddleSection(onLoginClick, onGoogleLoginClick)
		BottomSection(onNavigateToRegister)
	}

}

@Composable
fun TopSection(
	username: String,
	password: String,
	usernameError: String?,
	passwordError: String?,
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
				contentDescription = stringResource(R.string.cd_logo),
				modifier = Modifier
					.size(38.dp),
			)
		}

		Spacer(modifier = Modifier.height(24.dp))

		Text(
			text = stringResource(R.string.login),
			style = MaterialTheme.typography.headlineLarge,
		)

		Spacer(modifier = Modifier.height(24.dp))

		UsernameField(username, usernameError, onUsernameChange)

		Spacer(modifier = Modifier.height(16.dp))

		PasswordField(password, passwordError, onPasswordChange)

		Spacer(modifier = Modifier.height(8.dp))

		ForgotPasswordText(onForgotPasswordClick)

	}
}

@Composable
fun MiddleSection(onLoginClick: () -> Unit, onGoogleLoginClick: () -> Unit) {
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
				containerColor = MaterialTheme.colorScheme.primary,
				contentColor = MaterialTheme.colorScheme.onPrimary
			)
		) {
			Text(
				text = stringResource(R.string.login),
				fontSize = 14.sp
			)
		}

		Spacer(modifier = Modifier.height(24.dp))

		OrLoginWith()

		Spacer(modifier = Modifier.height(16.dp))

		OAuthButtonsRow(onGoogleLoginClick = onGoogleLoginClick)
	}
}


@Composable
fun BottomSection(onNavigateToRegister: () -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.Center
	) {
		Text(
			text = stringResource(R.string.no_account),
			style = MaterialTheme.typography.bodySmall
		)
		Spacer(modifier = Modifier.width(4.dp))
		Text(
			text = stringResource(R.string.sign_up),
			color = MaterialTheme.colorScheme.onBackground,
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
	errorMessage: String?,
	onUsernameChange: (String) -> Unit
) {
	OutlinedTextField(
		value = username,
		onValueChange = { onUsernameChange(it.removeLineBreaks()) },
		label = { Text(stringResource(R.string.username)) },
		isError = errorMessage != null,
		supportingText = errorMessage?.let { { Text(it) } },
		trailingIcon = {
			if (username.isNotEmpty()) {
				Icon(
					imageVector = Icons.Default.Check,
					contentDescription = stringResource(R.string.cd_valid_username)
				)
			}
		},
		modifier = Modifier.fillMaxWidth(),
		singleLine = true,
		keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
	)
}

@Composable
fun PasswordField(
	password: String,
	errorMessage: String?,
	onPasswordChange: (String) -> Unit
) {
	var isVisible by remember { mutableStateOf(false) }

	OutlinedTextField(
		value = password,
		onValueChange = { onPasswordChange(it.removeLineBreaks()) },
		label = { Text(stringResource(R.string.password)) },
		isError = errorMessage != null,
		supportingText = errorMessage?.let { { Text(it) } },
		visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
		trailingIcon = {
			IconButton(
				onClick = { isVisible = !isVisible }
			) {
				Icon(
					painter = if (isVisible) painterResource(R.drawable.eye_solid_full) else painterResource(
						R.drawable.eye_slash_solid_full
					),
					contentDescription = stringResource(R.string.cd_toggle_password),
					modifier = Modifier.size(24.dp)
				)
			}
		},
		modifier = Modifier.fillMaxWidth(),
		singleLine = true,
		keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
	)
}

private fun String.removeLineBreaks(): String =
	replace("\n", "").replace("\r", "")

@Composable
fun ForgotPasswordText(onForgotPasswordClick: () -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.End
	) {
		TextButton(onClick = onForgotPasswordClick) {
			Text(
				text = stringResource(R.string.forgot_password),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onBackground
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
			text = stringResource(R.string.or_login_with),
			style = MaterialTheme.typography.bodySmall
		)
		HorizontalDivider(modifier = Modifier.weight(1f))
	}
}

@Composable
fun OAuthButtonsRow(onGoogleLoginClick: () -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.Center
	) {
		OAuthButton(R.drawable.google_g_logo, onClick = onGoogleLoginClick )
	}
}

@Composable
fun OAuthButton(resId: Int, onClick: () -> Unit) {
	OutlinedButton(
		onClick = { onClick() },
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
	NarrativTheme {
		LoginScreen(
			username = "John Doe",
			password = "",
			usernameError = null,
			passwordError = null,
			onUsernameChange = {},
			onPasswordChange = {},
			onLoginClick = {},
			onNavigateToRegister = {},
			onForgotPasswordClick = {},
			onGoogleLoginClick = {}
		)
	}
}
