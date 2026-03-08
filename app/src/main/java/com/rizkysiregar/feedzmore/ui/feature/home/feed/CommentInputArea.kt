package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 08/03/26
 * Description: Software Engineer
 */
@Composable
fun CommentInputArea(
	text: String,
	onValueChange: (String) -> Unit,
	onSend: () -> Unit
) {
	Surface(
		tonalElevation = 2.dp,
		modifier = Modifier.imePadding() // Keyboard support
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			TextField(
				value = text,
				onValueChange = onValueChange,
				placeholder = { Text("Add a comment...") },
				modifier = Modifier.weight(1f),
				colors = TextFieldDefaults.colors(
					focusedContainerColor = Color.Transparent,
					unfocusedContainerColor = Color.Transparent,
					focusedIndicatorColor = Color.Transparent,
					unfocusedIndicatorColor = Color.Transparent
				),
				maxLines = 4
			)

			IconButton(
				onClick = onSend,
				enabled = text.isNotBlank(),
				colors = IconButtonDefaults.iconButtonColors(
					contentColor = MaterialTheme.colorScheme.primary
				)
			) {
				Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
			}
		}
	}
}


@Preview(showBackground = true)
@Composable
private fun CommentInputAreaPreview() {
	FeedzmoreTheme {
		CommentInputArea(
			text = "This is a comment",
			onValueChange = {},
			onSend = {}
		)
	}
}

