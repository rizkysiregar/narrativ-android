package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.narrativ.ui.components.EmojiPanel
import com.rizkysiregar.narrativ.ui.components.EmojiToggleButton
import com.rizkysiregar.narrativ.ui.components.insertEmoji
import com.rizkysiregar.narrativ.ui.components.rememberEmojiPanelState
import com.rizkysiregar.narrativ.ui.theme.NarrativTheme
import androidx.compose.ui.res.stringResource
import com.rizkysiregar.narrativ.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 08/03/26
 */

// the server stores a comment in VARCHAR(255): longer text would be rejected with a server error
const val MAX_COMMENT_LENGTH = 255
private const val COUNTER_THRESHOLD = 200
// the counter turns red when this few characters are left
private const val COUNTER_WARNING_LEFT = 20

@Composable
fun CommentInputArea(
	text: TextFieldValue,
	onValueChange: (TextFieldValue) -> Unit,
	onSend: () -> Unit,
	modifier: Modifier = Modifier,
	// username of the author being answered, null for a plain comment
	replyingTo: String? = null,
	onCancelReply: () -> Unit = {},
	// words of the caption the comment quotes, null for no quote
	quoting: String? = null,
	onCancelQuote: () -> Unit = {},
	isSubmitting: Boolean = false,
	focusRequester: FocusRequester = remember { FocusRequester() }
) {
	val emojiPanel = rememberEmojiPanelState()

	Surface(
		tonalElevation = 2.dp,
		modifier = modifier
	) {
		// keyboard first, then the nav bar: only the part of the nav bar the keyboard does not cover is added,
		// so the input sits directly on top of the keyboard. While the emoji panel is open the panel takes the
		// keyboard's place, so the keyboard inset is ignored: the input stays where it was instead of jumping
		Column(
			modifier = Modifier
				.then(if (emojiPanel.isOpen) Modifier else Modifier.imePadding())
				.navigationBarsPadding()
		) {
			if (replyingTo != null) {
				ReplyBanner(username = replyingTo, onCancel = onCancelReply)
			} else if (quoting != null) {
				QuoteBanner(quote = quoting, onCancel = onCancelQuote)
			}

			// the full list has its own recents, the quick row would only take height from the comments
			if (!emojiPanel.isOpen) {
				EmojiQuickRow(
					onEmojiClick = { emoji -> insertEmoji(text, emoji, MAX_COMMENT_LENGTH)?.let(onValueChange) },
					modifier = Modifier.fillMaxWidth()
				)
			}

			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				TextField(
					shape = RoundedCornerShape(24.dp),
					value = text,
					onValueChange = { if (it.text.length <= MAX_COMMENT_LENGTH) onValueChange(it) },
					placeholder = {
						Text(if (replyingTo != null) stringResource(R.string.reply_to, replyingTo) else stringResource(R.string.add_comment))
					},
					leadingIcon = { EmojiToggleButton(state = emojiPanel, focusRequester = focusRequester) },
					supportingText = if (text.text.length >= COUNTER_THRESHOLD) {
						{
							val nearLimit = MAX_COMMENT_LENGTH - text.text.length <= COUNTER_WARNING_LEFT
							Text(
								text = "${text.text.length}/$MAX_COMMENT_LENGTH",
								color = if (nearLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
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
					enabled = text.text.isNotBlank() && !isSubmitting
				) {
					if (isSubmitting) {
						CircularProgressIndicator(
							modifier = Modifier.size(20.dp),
							strokeWidth = 2.dp,
							color = MaterialTheme.colorScheme.onPrimary
						)
					} else {
						Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
					}
				}
			}

			if (emojiPanel.isOpen) {
				EmojiPanel(
					state = emojiPanel,
					onEmojiPicked = { emoji -> insertEmoji(text, emoji, MAX_COMMENT_LENGTH)?.let(onValueChange) },
					modifier = Modifier.fillMaxWidth()
				)
			}
		}
	}
}

@Composable
private fun ReplyBanner(username: String, onCancel: () -> Unit) {
	val replyingToLabel = stringResource(R.string.replying_to)
	Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = buildAnnotatedString {
					append(replyingToLabel)
					withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("@$username") }
				},
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.weight(1f)
			)
			IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
				Icon(
					Icons.Default.Close,
					contentDescription = stringResource(R.string.cancel_reply),
					modifier = Modifier.size(18.dp)
				)
			}
		}
	}
}


@Composable
private fun QuoteBanner(quote: String, onCancel: () -> Unit) {
	val quotingLabel = stringResource(R.string.quoting)
	Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = buildAnnotatedString {
					withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(quotingLabel) }
					append("  “$quote”")
				},
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				maxLines = 2,
				overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
				modifier = Modifier.weight(1f)
			)
			IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
				Icon(
					Icons.Default.Close,
					contentDescription = stringResource(R.string.cancel_quote),
					modifier = Modifier.size(18.dp)
				)
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun CommentInputAreaPreview() {
	NarrativTheme {
		CommentInputArea(
			text = TextFieldValue("This is a comment"),
			onValueChange = {},
			onSend = {}
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun CommentInputAreaReplyingPreview() {
	NarrativTheme {
		CommentInputArea(
			text = TextFieldValue("Setuju"),
			onValueChange = {},
			onSend = {},
			replyingTo = "budi"
		)
	}
}
