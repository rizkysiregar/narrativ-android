package com.rizkysiregar.narrativ.ui.feature.auth.register

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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import com.rizkysiregar.narrativ.ui.feature.legal.LegalDocument
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
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.domain.user.UsernameRules
import com.rizkysiregar.narrativ.ui.theme.NarrativTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources


/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 24/02/26
 */
@Composable
fun RegisterScreen(
	onRegisterClick: (RegisterState) -> Unit,
	onNavigateToLogin: () -> Unit,
	onLegalClick: (LegalDocument) -> Unit = {},
	onGoogleSignInClick: () -> Unit = {},
	initialState: RegisterState = RegisterState()
) {
	val context = LocalContext.current
	val resources = LocalResources.current
	var registerState by remember { mutableStateOf(initialState) }
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
			onLegalClick = onLegalClick,
			onGoogleSignInClick = onGoogleSignInClick,
			onRegisterClick = {
				val errors = registerState.validate(
					required = { label -> resources.getString(R.string.error_required, resources.getString(label)) },
					usernameFormat = resources.getString(R.string.error_username_format)
				)
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
				contentDescription = stringResource(R.string.cd_logo),
				modifier = Modifier
					.size(38.dp),
			)
		}

		Text(
			text = stringResource(R.string.sign_up),
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
				label = { Text(stringResource(R.string.first_name)) },
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
				label = { Text(stringResource(R.string.last_name)) },
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
			label = { Text(stringResource(R.string.email)) },
			isError = formErrors.email != null,
			supportingText = formErrors.email?.let { { Text(it) } },
			modifier = Modifier.fillMaxWidth(),
			singleLine = true,
			keyboardOptions = KeyboardOptions(
				keyboardType = KeyboardType.Email,
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
fun MiddleSection(
	onRegisterClick: () -> Unit,
	onLegalClick: (LegalDocument) -> Unit = {},
	onGoogleSignInClick: () -> Unit = {}
) {
	Column {
		LegalConsentText(onLegalClick)

		Spacer(modifier = Modifier.height(12.dp))

		Button(
			onClick = {
				onRegisterClick()
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
				text = stringResource(R.string.sign_up),
				fontSize = 14.sp
			)
		}

		Spacer(modifier = Modifier.height(24.dp))

		OrLoginWith()

		Spacer(modifier = Modifier.height(16.dp))

		OAuthButtonsRow(onGoogleSignInClick = onGoogleSignInClick)
	}
}

/**
 * Signing up, with an email or with Google, means accepting the terms; both documents open inside the app.
 */
@Composable
private fun LegalConsentText(onLegalClick: (LegalDocument) -> Unit) {
	val language = LegalDocument.deviceLanguage()
	val linkStyle = TextLinkStyles(
		style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
	)
	fun link(document: LegalDocument) = LinkAnnotation.Clickable(
		tag = document.path,
		styles = linkStyle,
		linkInteractionListener = { onLegalClick(document) }
	)

	val text = buildAnnotatedString {
		append(stringResource(R.string.legal_consent_prefix))
		withLink(link(LegalDocument.TERMS)) { append(LegalDocument.TERMS.title(language)) }
		append(stringResource(R.string.legal_consent_and))
		withLink(link(LegalDocument.PRIVACY_POLICY)) { append(LegalDocument.PRIVACY_POLICY.title(language)) }
		append(".")
	}
	Text(
		text = text,
		style = MaterialTheme.typography.bodySmall,
		color = MaterialTheme.colorScheme.onSurfaceVariant,
		textAlign = TextAlign.Center,
		modifier = Modifier.fillMaxWidth()
	)
}

@Composable
fun BottomSection(onNavigateToLogin: () -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.Center
	) {
		Text(
			text = stringResource(R.string.already_account),
			style = MaterialTheme.typography.bodySmall
		)
		Spacer(modifier = Modifier.width(4.dp))
		Text(
			text = stringResource(R.string.login_here),
			color = MaterialTheme.colorScheme.onBackground,
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
		label = { Text(stringResource(R.string.username)) },
		isError = errorMessage != null,
		supportingText = errorMessage?.let { { Text(it) } },
		trailingIcon = {
			if (UsernameRules.isValid(username.trim())) {
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

private data class RegisterFormErrors(
	val firstName: String? = null,
	val lastName: String? = null,
	val username: String? = null,
	val email: String? = null,
	val password: String? = null
) {
	val hasErrors: Boolean
		get() = listOf(firstName, lastName, username, email, password).any { it != null }
}

private fun RegisterState.validate(
	required: (labelRes: Int) -> String,
	usernameFormat: String
): RegisterFormErrors = RegisterFormErrors(
	firstName = firstName.requiredError(required(R.string.first_name)),
	lastName = lastName.requiredError(required(R.string.last_name)),
	username = username.requiredError(required(R.string.username))
		?: usernameFormat.takeUnless { UsernameRules.isValid(username.trim()) },
	email = email.requiredError(required(R.string.email)),
	password = password.requiredError(required(R.string.password))
)

private fun RegisterFormErrors.clearResolvedErrors(state: RegisterState): RegisterFormErrors = copy(
	firstName = firstName?.takeIf { state.firstName.isBlank() },
	lastName = lastName?.takeIf { state.lastName.isBlank() },
	username = username?.takeIf { state.username.isBlank() },
	email = email?.takeIf { state.email.isBlank() },
	password = password?.takeIf { state.password.isBlank() }
)

/** [message] is the full, already translated "X is required". */
private fun String.requiredError(message: String): String? =
	if (isBlank()) message else null

private fun RegisterState.trimmed(): RegisterState = copy(
	firstName = firstName.trim(),
	lastName = lastName.trim(),
	username = username.trim(),
	email = email.trim()
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
			text = stringResource(R.string.forgot_password),
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onBackground,
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
			text = stringResource(R.string.or_login_with),
			style = MaterialTheme.typography.bodySmall
		)
		HorizontalDivider(modifier = Modifier.weight(1f))
	}
}

@Composable
fun OAuthButtonsRow(onGoogleSignInClick: () -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.Center
	) {
		OAuthButton(R.drawable.google_g_logo, onClick = onGoogleSignInClick)
	}
}

@Composable
fun OAuthButton(resId: Int, onClick: () -> Unit) {
	OutlinedButton(
		onClick = onClick,
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
		RegisterScreen(
			onRegisterClick = {},
			onNavigateToLogin = {}
		)
	}
}
