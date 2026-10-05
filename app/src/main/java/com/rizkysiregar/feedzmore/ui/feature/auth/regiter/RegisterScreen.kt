package com.rizkysiregar.feedzmore.ui.feature.auth.regiter

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizkysiregar.feedzmore.R
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme


/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 24/02/26
 * Description: Software Engineer
 */
@Composable
fun RegisterScreen(
	onRegisterClick: (RegisterState) -> Unit,
	onNavigateToLogin: () -> Unit
) {
	var registerState by remember { mutableStateOf(RegisterState()) }
	var formErrors by remember { mutableStateOf(RegisterFormErrors()) }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(WindowInsets.systemBars.asPaddingValues())
			.padding(16.dp),
		verticalArrangement = Arrangement.SpaceBetween
	) {
		TopSection(
			registerState = registerState,
			formErrors = formErrors,
			onStateChange = { newState ->
				registerState = newState
				formErrors = formErrors.clearResolvedErrors(newState)
			}
		)
		MiddleSection(
			onRegisterClick = {
				val errors = registerState.validate()
				formErrors = errors
				if (!errors.hasErrors) {
					onRegisterClick(registerState.trimmed())
				}
			}
		)
		BottomSection(onNavigateToLogin)
	}

}

@Composable
private fun TopSection(
	registerState: RegisterState,
	formErrors: RegisterFormErrors,
	onStateChange: (RegisterState) -> Unit
) {
	Column {
		// -- Header -- ///
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

		Text(
			text = "Sign Up",
			style = MaterialTheme.typography.headlineLarge,
		)

		Spacer(modifier = Modifier.height(24.dp))

		// -- Register Field -- //

		// -- First Name & Last Name -- //
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			OutlinedTextField(
				value = registerState.firstName,
				onValueChange = { newFirstName ->
					onStateChange(registerState.copy(firstName = newFirstName.removeLineBreaks()))
				},
				label = { Text("First Name") },
				isError = formErrors.firstName != null,
				supportingText = formErrors.firstName?.let { { Text(it) } },
				modifier = Modifier.weight(1f),
				singleLine = true,
				keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
			)
			OutlinedTextField(
				value = registerState.lastName,
				onValueChange = { newLastName ->
					onStateChange(registerState.copy(lastName = newLastName.removeLineBreaks()))
				},
				label = { Text("Last Name") },
				isError = formErrors.lastName != null,
				supportingText = formErrors.lastName?.let { { Text(it) } },
				modifier = Modifier.weight(1f),
				singleLine = true,
				keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
			)
		}

		// -- Email -- //
		Spacer(modifier = Modifier.height(16.dp))
		OutlinedTextField(
			value = registerState.email,
			onValueChange = { newEmail ->
				onStateChange(registerState.copy(email = newEmail.removeLineBreaks()))
			},
			label = { Text("Email") },
			isError = formErrors.email != null,
			supportingText = formErrors.email?.let { { Text(it) } },
			modifier = Modifier.fillMaxWidth(),
			singleLine = true,
			keyboardOptions = KeyboardOptions(
				keyboardType = KeyboardType.Email,
				imeAction = ImeAction.Next
			)
		)

		// -- Phone Number -- //
		Spacer(modifier = Modifier.height(16.dp))
		OutlinedTextField(
			value = registerState.phoneNumber,
			onValueChange = { newPhoneNumber ->
				onStateChange(registerState.copy(phoneNumber = newPhoneNumber.removeLineBreaks()))
			},
			label = { Text("Phone Number") },
			isError = formErrors.phoneNumber != null,
			supportingText = formErrors.phoneNumber?.let { { Text(it) } },
			modifier = Modifier.fillMaxWidth(),
			singleLine = true,
			keyboardOptions = KeyboardOptions(
				keyboardType = KeyboardType.Phone,
				imeAction = ImeAction.Next
			)
		)

		// -- Username -- //
		Spacer(modifier = Modifier.height(16.dp))
		UsernameField(
			username = registerState.username,
			errorMessage = formErrors.username,
			onUsernameChange = { newUsername ->
				onStateChange(registerState.copy(username = newUsername))
			}
		)

		// -- Password -- //
		Spacer(modifier = Modifier.height(16.dp))
		PasswordField(
			password = registerState.password,
			errorMessage = formErrors.password,
			onPasswordChange = { newPassword ->
				onStateChange(registerState.copy(password = newPassword))
			}
		)
	}
}

@Composable
fun MiddleSection(onRegisterClick: () -> Unit) {
	Column {
		Button(
			onClick = {
				onRegisterClick()
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
				text = "Sign Up",
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
fun BottomSection(onNavigateToLogin: () -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.Center
	) {
		Text(
			text = "Already have an account?",
			style = MaterialTheme.typography.bodySmall
		)
		Spacer(modifier = Modifier.width(4.dp))
		Text(
			text = "Login here",
			color = Color.Black,
			style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
			modifier = Modifier.clickable(
				interactionSource = remember { MutableInteractionSource() },
				indication = ripple()
			) {
				onNavigateToLogin()
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
		label = { Text("Username") },
		isError = errorMessage != null,
		supportingText = errorMessage?.let { { Text(it) } },
		trailingIcon = {
			if (username.isNotEmpty()) {
				Icon(
					imageVector = Icons.Default.Check,
					contentDescription = "Valid Username"
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
		label = { Text("Password") },
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
					contentDescription = "Toggle password visibility",
					modifier = Modifier.size(24.dp)
				)
			}
		},
		modifier = Modifier.fillMaxWidth(),
		singleLine = true,
		keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
	)
}

private data class RegisterFormErrors(
	val firstName: String? = null,
	val lastName: String? = null,
	val username: String? = null,
	val email: String? = null,
	val password: String? = null,
	val phoneNumber: String? = null
) {
	val hasErrors: Boolean
		get() = listOf(firstName, lastName, username, email, password, phoneNumber).any { it != null }
}

private fun RegisterState.validate(): RegisterFormErrors = RegisterFormErrors(
	firstName = firstName.requiredError("Nama depan"),
	lastName = lastName.requiredError("Nama belakang"),
	username = username.requiredError("Username"),
	email = email.requiredError("Email"),
	password = password.requiredError("Password"),
	phoneNumber = phoneNumber.requiredError("Nomor telepon")
)

private fun RegisterFormErrors.clearResolvedErrors(state: RegisterState): RegisterFormErrors = copy(
	firstName = firstName?.takeIf { state.firstName.isBlank() },
	lastName = lastName?.takeIf { state.lastName.isBlank() },
	username = username?.takeIf { state.username.isBlank() },
	email = email?.takeIf { state.email.isBlank() },
	password = password?.takeIf { state.password.isBlank() },
	phoneNumber = phoneNumber?.takeIf { state.phoneNumber.isBlank() }
)

private fun String.requiredError(label: String): String? =
	if (isBlank()) "$label wajib diisi" else null

private fun RegisterState.trimmed(): RegisterState = copy(
	firstName = firstName.trim(),
	lastName = lastName.trim(),
	username = username.trim(),
	email = email.trim(),
	phoneNumber = phoneNumber.trim()
)

private fun String.removeLineBreaks(): String =
	replace("\n", "").replace("\r", "")

@Composable
fun ForgotPasswordText() {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.End
	) {
		Text(
			text = "Forgot Password?",
			style = MaterialTheme.typography.bodySmall,
			color = Color.Black,
		)
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
		RegisterScreen(
			onRegisterClick = {},
			onNavigateToLogin = {}
		)
	}
}
