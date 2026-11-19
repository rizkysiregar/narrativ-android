package com.rizkysiregar.narrativ.ui.feature.home.thread

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import com.rizkysiregar.narrativ.R

/**
 * The caption of a thread, selectable like text in a document. Selecting some of it offers "Quote & reply", which
 * hands the selected words to [onQuote]. [highlight] (words a comment quoted) is marked in the caption.
 */
@Composable
internal fun QuotableCaption(
	caption: String,
	style: TextStyle,
	highlight: String?,
	onQuote: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	// a read-only text field, not a Text: only a field tells which part of the text is selected
	var value by remember(caption) { mutableStateOf(TextFieldValue(caption)) }
	val selected = value.selection.takeUnless { it.collapsed }?.let { caption.substring(it.min, it.max) }?.trim()
	val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
	val transformation = remember(highlight, highlightColor) { HighlightTransformation(highlight, highlightColor) }

	Column(modifier = modifier) {
		BasicTextField(
			value = value,
			onValueChange = { value = it.copy(text = caption) },
			readOnly = true,
			textStyle = style.copy(color = MaterialTheme.colorScheme.onSurface),
			cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
			visualTransformation = transformation
		)
		AnimatedVisibility(visible = !selected.isNullOrEmpty()) {
			FilledTonalButton(
				onClick = {
					selected?.let(onQuote)
					value = value.copy(selection = TextRange(value.selection.max))
				},
				modifier = Modifier.padding(top = 8.dp)
			) {
				Icon(Icons.Filled.FormatQuote, contentDescription = null, modifier = Modifier.size(18.dp))
				Spacer(Modifier.width(6.dp))
				Text(stringResource(R.string.quote_and_reply))
			}
		}
	}
}

/** Marks the first place [words] appear in the text; the text itself and its offsets stay the same. */
private class HighlightTransformation(
	private val words: String?,
	private val color: androidx.compose.ui.graphics.Color
) : VisualTransformation {
	override fun filter(text: AnnotatedString): TransformedText {
		val start = words?.takeIf { it.isNotBlank() }?.let { text.text.indexOf(it) } ?: -1
		if (start < 0) return TransformedText(text, OffsetMapping.Identity)
		val marked = AnnotatedString.Builder(text).apply {
			addStyle(SpanStyle(background = color), start, start + words!!.length)
		}.toAnnotatedString()
		return TransformedText(marked, OffsetMapping.Identity)
	}

	override fun equals(other: Any?): Boolean =
		other is HighlightTransformation && other.words == words && other.color == color

	override fun hashCode(): Int = 31 * words.hashCode() + color.hashCode()
}
