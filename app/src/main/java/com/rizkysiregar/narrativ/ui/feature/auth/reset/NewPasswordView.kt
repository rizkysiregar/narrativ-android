package com.rizkysiregar.narrativ.ui.feature.auth.reset

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.data.remote.model.request.auth.ResetNewPasswordRequest
import com.rizkysiregar.narrativ.ui.theme.NarrativTheme
import androidx.compose.ui.res.stringResource

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 02/03/26
 * Description: Software Engineer
 */

@Composable
fun NewPasswordView(
	onNext: (ResetNewPasswordRequest) -> Unit,
	resetToken: String,
	email: String
) {
	var newPassword by rememberSaveable { mutableStateOf("") }
	var confirmNewPassword by rememberSaveable { mutableStateOf("") }
	val passwordsMatch = confirmNewPassword == newPassword
	val canSubmit = newPassword.length >= 6 && passwordsMatch && resetToken.isNotBlank()


	Column(
		modifier = Modifier
			.background(color = MaterialTheme.colorScheme.background)
			.fillMaxSize()
			.padding(16.dp),
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.End
		) {
			Image(
				painter = painterResource(R.drawable.star_black),
				contentDescription = stringResource(R.string.cd_logo),
				modifier = Modifier.size(38.dp)
			)
		}

		// -- Header Title and caption -- //
		Spacer(Modifier.height(16.dp))
		Column {
			Text(
				text = stringResource(R.string.reset_password),
				style = MaterialTheme.typography.headlineLarge
			)
			Spacer(Modifier.height(8.dp))
			Text(
				text = stringResource(R.string.new_password_hint),
				style = MaterialTheme.typography.bodyMedium
			)
		}


		// --- Input fields --- //
		Spacer(Modifier.height(32.dp))
		Column {
			Text(stringResource(R.string.new_password), style = MaterialTheme.typography.labelSmall)
			OutlinedTextField(
				value = newPassword,
				onValueChange = { newPassword = it },
				label = { Text(stringResource(R.string.password_min_length)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				visualTransformation = PasswordVisualTransformation(),
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
			)

			Spacer(Modifier.height(16.dp))
			Text(stringResource(R.string.confirm_new_password), style = MaterialTheme.typography.labelSmall)
			OutlinedTextField(
				value = confirmNewPassword,
				onValueChange = { confirmNewPassword = it },
				label = { Text(stringResource(R.string.repeat_password)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				visualTransformation = PasswordVisualTransformation(),
				isError = confirmNewPassword.isNotEmpty() && !passwordsMatch,
				supportingText = if (confirmNewPassword.isNotEmpty() && !passwordsMatch) {
					{ Text(stringResource(R.string.error_passwords_mismatch)) }
				} else null,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
			)

			Spacer(Modifier.height(24.dp))
			Button(
				onClick = {
					onNext(
						ResetNewPasswordRequest(
							email = email,
							newPassword = newPassword,
							resetToken = resetToken
						)
					)
				},
				enabled = canSubmit,
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
					text = stringResource(R.string.reset_password),
					fontSize = 14.sp
				)
			}
		}
	}
}


@Preview
@Composable
private fun NewPasswordScreenPreview() {
	NarrativTheme {
		NewPasswordView(
			onNext = {},
			resetToken = "",
			email = ""
		)
	}
}