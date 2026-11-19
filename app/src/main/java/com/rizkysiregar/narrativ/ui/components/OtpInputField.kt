package com.rizkysiregar.narrativ.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.narrativ.ui.theme.NarrativTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 02/03/26
 * Description: Software Engineer
 */

private const val OTP_LENGTH = 6

/**
 * One text field drawn as [OTP_LENGTH] boxes, so typing, backspace and pasting the whole code behave like a normal
 * field. The cursor is always kept at the end: a box can not be edited on its own, the code is typed left to right.
 */
@Composable
fun OtpInputField(
	otpText: String,
	onOtpTextChange: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	val focusRequester = remember { FocusRequester() }
	var isFocused by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) { focusRequester.requestFocus() }

	BasicTextField(
		value = TextFieldValue(otpText, selection = TextRange(otpText.length)),
		onValueChange = { value ->
			onOtpTextChange(value.text.filter(Char::isDigit).take(OTP_LENGTH))
		},
		modifier = modifier
			.focusRequester(focusRequester)
			.onFocusChanged { isFocused = it.isFocused },
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
		singleLine = true,
		decorationBox = { innerTextField ->
			// the real field still has to be composed to receive input, it is only hidden behind the boxes
			Box {
				Box(Modifier.size(1.dp).alpha(0f)) { innerTextField() }
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					repeat(OTP_LENGTH) { index ->
						val isCurrent = isFocused && index == otpText.length.coerceAtMost(OTP_LENGTH - 1)
						Box(
							modifier = Modifier
								.size(45.dp)
								.border(
									width = if (isCurrent) 2.dp else 1.dp,
									color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
									shape = RoundedCornerShape(8.dp)
								),
							contentAlignment = Alignment.Center
						) {
							Text(
								text = otpText.getOrNull(index)?.toString().orEmpty(),
								style = MaterialTheme.typography.titleLarge,
								color = MaterialTheme.colorScheme.onBackground,
								textAlign = TextAlign.Center
							)
						}
					}
				}
			}
		}
	)
}


@Preview
@Composable
private fun OtpInputFieldPreview() {
	NarrativTheme {
		OtpInputField(
			otpText = "123",
			onOtpTextChange = {}
		)
	}
}
