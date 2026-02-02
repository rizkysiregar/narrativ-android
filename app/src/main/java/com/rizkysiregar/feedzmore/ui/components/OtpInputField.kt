package com.rizkysiregar.feedzmore.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 02/03/26
 * Description: Software Engineer
 */

@Composable
fun OtpInputField(
	otpText: String,
	onOtpTextChange: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	val focusRequesters = remember { List(6) { FocusRequester() } }

	Row(
		modifier = modifier,
		horizontalArrangement = Arrangement.spacedBy(8.dp)
	) {
		repeat(6) { index ->
			BasicTextField(
				value = otpText.getOrNull(index)?.toString() ?: "",
				onValueChange = { value ->
					// Handle input
					if (value.length <= 1) {
						val newText = otpText.toMutableList().apply {
							if (index < size) this[index] = value.firstOrNull() ?: ' '
							else add(value.firstOrNull() ?: ' ')
						}.joinToString("").trim()

						onOtpTextChange(newText)

						// Move forward
						if (value.isNotEmpty() && index < 5) focusRequesters[index + 1].requestFocus()
					} else if (value.length == 6) {
						// Handle paste functionality
						onOtpTextChange(value)
					}
				},
				modifier = Modifier
					.size(45.dp)
					.focusRequester(focusRequesters[index])
					.border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
				singleLine = true,
				textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
				decorationBox = { Box(contentAlignment = Alignment.Center) { it() } }
			)
		}
	}
}


@Preview
@Composable
private fun OtpInputFieldPreview() {
	FeedzmoreTheme {
		OtpInputField(
			otpText = "",
			onOtpTextChange = {}
		)
	}
}