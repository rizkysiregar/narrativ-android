package com.rizkysiregar.narrativ.ui.feature.home.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.ui.components.OtpInputField

/**
 * Settings > Change email. First the new address, then the code mailed to it together with the password (or the
 * Google account). [onChanged] runs once the new email and the new session are stored.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailRoute(
	viewModel: ChangeEmailViewModel = hiltViewModel(),
	onBackClick: () -> Unit,
	onChanged: () -> Unit
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()

	LaunchedEffect(uiState.done) {
		if (uiState.done) onChanged()
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.change_email)) },
				navigationIcon = {
					IconButton(onClick = onBackClick) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
						)
					}
				}
			)
		}
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 24.dp, vertical = 16.dp)
		) {
			when (uiState.step) {
				ChangeEmailUiState.Step.ENTER_EMAIL -> NewEmailStep(
					isBusy = uiState.isBusy,
					error = uiState.error,
					onEdit = viewModel::clearError,
					onSendCode = viewModel::sendCode
				)
				ChangeEmailUiState.Step.ENTER_CODE -> ConfirmStep(
					sentTo = uiState.sentTo.orEmpty(),
					isBusy = uiState.isBusy,
					error = uiState.error,
					onEdit = viewModel::clearError,
					onConfirmWithPassword = viewModel::confirmWithPassword,
					onConfirmWithGoogle = viewModel::confirmWithGoogle,
					onResend = viewModel::resendCode,
					onEditEmail = viewModel::editEmail
				)
			}
		}
	}
}

@Composable
private fun NewEmailStep(
	isBusy: Boolean,
	error: String?,
	onEdit: () -> Unit,
	onSendCode: (String) -> Unit
) {
	var email by rememberSaveable { mutableStateOf("") }

	Text(stringResource(R.string.change_email_intro), style = MaterialTheme.typography.bodyMedium)
	Spacer(Modifier.height(16.dp))
	OutlinedTextField(
		value = email,
		onValueChange = {
			email = it.replace("\n", "")
			onEdit()
		},
		label = { Text(stringResource(R.string.new_email)) },
		singleLine = true,
		enabled = !isBusy,
		isError = error != null,
		supportingText = error?.let { { Text(it) } },
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
		modifier = Modifier.fillMaxWidth()
	)
	Spacer(Modifier.height(16.dp))
	Button(
		enabled = !isBusy && email.isNotBlank(),
		onClick = { onSendCode(email) },
		modifier = Modifier.fillMaxWidth()
	) { Text(if (isBusy) stringResource(R.string.sending) else stringResource(R.string.send_code)) }
}

@Composable
private fun ConfirmStep(
	sentTo: String,
	isBusy: Boolean,
	error: String?,
	onEdit: () -> Unit,
	onConfirmWithPassword: (otp: String, password: String) -> Unit,
	onConfirmWithGoogle: (otp: String) -> Unit,
	onResend: () -> Unit,
	onEditEmail: () -> Unit
) {
	var otp by rememberSaveable { mutableStateOf("") }
	var password by rememberSaveable { mutableStateOf("") }
	val code = otp.filter(Char::isDigit)

	Text(stringResource(R.string.otp_sent_to, sentTo), style = MaterialTheme.typography.bodyMedium)
	Spacer(Modifier.height(16.dp))
	OtpInputField(
		otpText = otp,
		onOtpTextChange = {
			otp = it.filter(Char::isDigit).take(6)
			onEdit()
		}
	)
	Spacer(Modifier.height(24.dp))
	Text(stringResource(R.string.change_email_confirm_hint), style = MaterialTheme.typography.bodyMedium)
	Spacer(Modifier.height(8.dp))
	OutlinedTextField(
		value = password,
		onValueChange = {
			password = it
			onEdit()
		},
		label = { Text(stringResource(R.string.password)) },
		singleLine = true,
		enabled = !isBusy,
		visualTransformation = PasswordVisualTransformation(),
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
		isError = error != null,
		supportingText = error?.let { { Text(it) } },
		modifier = Modifier.fillMaxWidth()
	)
	Spacer(Modifier.height(16.dp))
	Button(
		enabled = !isBusy && code.length == 6 && password.isNotBlank(),
		onClick = { onConfirmWithPassword(code, password) },
		modifier = Modifier.fillMaxWidth()
	) { Text(if (isBusy) stringResource(R.string.saving) else stringResource(R.string.change_email)) }
	Spacer(Modifier.height(16.dp))
	Text(
		stringResource(R.string.google_no_password),
		style = MaterialTheme.typography.bodySmall,
		color = MaterialTheme.colorScheme.onSurfaceVariant
	)
	OutlinedButton(
		enabled = !isBusy && code.length == 6,
		onClick = { onConfirmWithGoogle(code) },
		modifier = Modifier.fillMaxWidth()
	) { Text(stringResource(R.string.verify_with_google)) }
	Spacer(Modifier.height(8.dp))
	TextButton(enabled = !isBusy, onClick = onResend) { Text(stringResource(R.string.resend_code)) }
	TextButton(enabled = !isBusy, onClick = onEditEmail) { Text(stringResource(R.string.change_email_edit_address)) }
}
