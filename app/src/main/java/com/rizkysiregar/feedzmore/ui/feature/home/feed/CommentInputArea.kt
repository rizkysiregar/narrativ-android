package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 08/03/26
 * Description: Software Engineer
 */

// the server stores a comment in VARCHAR(255): longer text would be rejected with a server error
const val MAX_COMMENT_LENGTH = 255
private const val COUNTER_THRESHOLD = 200

@Composable
fun CommentInputArea(
	text: String,
	onValueChange: (String) -> Unit,
	onSend: () -> Unit,
	modifier: Modifier = Modifier,
	// username of the author being answered, null for a plain comment
	replyingTo: String? = null,
	onCancelReply: () -> Unit = {},
	isSubmitting: Boolean = false,
	focusRequester: FocusRequester = remember { FocusRequester() }
) {
	Surface(
		tonalElevation = 2.dp,
		modifier = modifier.imePadding() // Keyboard support
	) {
		Column {
			if (replyingTo != null) {
				ReplyBanner(username = replyingTo, onCancel = onCancelReply)
			}

			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 8.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				TextField(
					shape = RoundedCornerShape(24.dp),
					value = text,
					onValueChange = { if (it.length <= MAX_COMMENT_LENGTH) onValueChange(it) },
					placeholder = {
						Text(if (replyingTo != null) "Balas @$replyingTo..." else "Tambahkan komentar...")
					},
					supportingText = if (text.length >= COUNTER_THRESHOLD) {
						{ Text("${text.length}/$MAX_COMMENT_LENGTH") }
					} else null,
					modifier = Modifier
						.weight(1f)
						.focusRequester(focusRequester),
					colors = TextFieldDefaults.colors(
						focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
						unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
						focusedIndicatorColor = Color.Transparent,
						unfocusedIndicatorColor = Color.Transparent
					),
					maxLines = 4
				)

				FilledIconButton(
					onClick = onSend,
					enabled = text.isNotBlank() && !isSubmitting
				) {
					if (isSubmitting) {
						CircularProgressIndicator(
							modifier = Modifier.size(20.dp),
							strokeWidth = 2.dp,
							color = MaterialTheme.colorScheme.onPrimary
						)
					} else {
						Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim")
					}
				}
			}
		}
	}
}

@Composable
private fun ReplyBanner(username: String, onCancel: () -> Unit) {
	Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = buildAnnotatedString {
					append("Membalas ")
					withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("@$username") }
				},
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.weight(1f)
			)
			IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
				Icon(
					Icons.Default.Close,
					contentDescription = "Batalkan balasan",
					modifier = Modifier.size(18.dp)
				)
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

@Preview(showBackground = true)
@Composable
private fun CommentInputAreaReplyingPreview() {
	FeedzmoreTheme {
		CommentInputArea(
			text = "Setuju",
			onValueChange = {},
			onSend = {},
			replyingTo = "budi"
		)
	}
}
