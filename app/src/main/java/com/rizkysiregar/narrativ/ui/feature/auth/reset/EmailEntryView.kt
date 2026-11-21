package com.rizkysiregar.narrativ.ui.feature.auth.reset

import android.util.Patterns
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.ui.theme.NarrativTheme
import androidx.compose.ui.res.stringResource

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 02/03/26
 */

@Composable
fun EmailEntryView(
	onNext: (String) -> Unit,
	onBack: () -> Unit
) {
	var email by remember { mutableStateOf("") }
	val isValidEmail = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

	Column(
		modifier = Modifier
			.background(color = MaterialTheme.colorScheme.background)
			.fillMaxSize()
			.padding(16.dp),
	) {

		// --- Header: Back Button & Logo --- //
		Row(
			modifier = Modifier
				.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Box(
				modifier = Modifier
					.padding(4.dp)
					.border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
					.size(35.dp)
					.clickable { onBack() },
				contentAlignment = Alignment.Center
			) {
				Icon(
					painter = painterResource(R.drawable.outline_arrow_back_ios_24),
					contentDescription = stringResource(R.string.back),
					modifier = Modifier
						.size(24.dp)
						.padding(3.dp)
				)
			}

			Image(
				painter = painterResource(R.drawable.star_black),
				contentDescription = stringResource(R.string.cd_logo),
				modifier = Modifier
					.size(38.dp),
			)

		}

		// --- Header Title and caption --- //
		Spacer(Modifier.height(16.dp))
		Column {
			Text(
				text = stringResource(R.string.forgot_password),
				style = MaterialTheme.typography.headlineLarge
			)
			Spacer(Modifier.height(8.dp))
			Text(
				text = stringResource(R.string.forgot_password_hint),
				style = MaterialTheme.typography.bodyMedium
			)
		}


		// --- Inout field --- //
		Spacer(Modifier.height(32.dp))
		Column {
			Text(stringResource(R.string.email_address), style = MaterialTheme.typography.labelSmall)
			OutlinedTextField(
				value = email,
				onValueChange = {
					email = it.filterNot(Char::isWhitespace).take(254)
				},
				label = { Text(stringResource(R.string.enter_email_address)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				isError = email.isNotEmpty() && !isValidEmail,
				supportingText = if (email.isNotEmpty() && !isValidEmail) {
					{ Text(stringResource(R.string.error_invalid_email)) }
				} else null,
				keyboardOptions = KeyboardOptions(
					keyboardType = KeyboardType.Email,
					imeAction = ImeAction.Done
				),
				keyboardActions = KeyboardActions(onDone = {
					if (isValidEmail) onNext(email)
				})
			)
			Spacer(Modifier.height(24.dp))
			Button(
				onClick = { onNext(email) },
				enabled = isValidEmail,
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
					text = stringResource(R.string.send_code),
					fontSize = 14.sp
				)
			}
		}
	}
}


@Preview
@Composable
private fun EmailEntryViewPreview() {
	NarrativTheme {
		EmailEntryView(
			onNext = {},
			onBack = {}
		)
	}
}
