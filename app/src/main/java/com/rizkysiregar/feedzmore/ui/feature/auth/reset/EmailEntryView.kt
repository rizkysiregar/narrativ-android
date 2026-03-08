package com.rizkysiregar.feedzmore.ui.feature.auth.reset

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizkysiregar.feedzmore.R
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 02/03/26
 * Description: Software Engineer
 */

@Composable
fun EmailEntryView(
	onNext: (String) -> Unit,
	onBack: () -> Unit
) {
	var email by remember { mutableStateOf("") }

	Column(
		modifier = Modifier
			.background(color = Color.White)
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
					contentDescription = "Back",
					modifier = Modifier
						.size(24.dp)
						.padding(3.dp)
				)
			}

			Image(
				painter = painterResource(R.drawable.star_black),
				contentDescription = "Header Logo",
				modifier = Modifier
					.size(38.dp),
			)

		}

		// --- Header Title and caption --- //
		Spacer(Modifier.height(16.dp))
		Column {
			Text(
				text = "Forgot Password?",
				style = MaterialTheme.typography.headlineLarge
			)
			Spacer(Modifier.height(8.dp))
			Text(
				text = "Don’t worry! It happens. Please enter the email associated with your account.",
				style = MaterialTheme.typography.bodyMedium
			)
		}


		// --- Inout field --- //
		Spacer(Modifier.height(32.dp))
		Column {
			Text("Email Address", style = MaterialTheme.typography.labelSmall)
			OutlinedTextField(
				value = email,
				onValueChange = {
					email = it
				},
				label = { Text("Enter your Email Address") },
				modifier = Modifier.fillMaxWidth(),
			)
			Spacer(Modifier.height(24.dp))
			Button(
				onClick = {
					onNext(email)
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
					text = "Send Code",
					fontSize = 14.sp
				)
			}
		}
	}
}


@Preview
@Composable
private fun EmailEntryViewPreview() {
	FeedzmoreTheme {
		EmailEntryView(
			onNext = {},
			onBack = {}
		)
	}
}