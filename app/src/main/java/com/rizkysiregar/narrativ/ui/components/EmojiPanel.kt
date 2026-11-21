package com.rizkysiregar.narrativ.ui.components

import android.view.ContextThemeWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import com.rizkysiregar.narrativ.R
import androidx.compose.ui.res.stringResource

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 */

private val DEFAULT_PANEL_HEIGHT = 280.dp
private val MIN_PANEL_HEIGHT = 220.dp
// keeps the content above the panel readable
private const val MAX_PANEL_SCREEN_FRACTION = 0.4f

/**
 * Whether the full emoji list is shown in place of the keyboard, and how tall it should be.
 * While [isOpen] the screen should skip its keyboard inset: the panel takes the keyboard's place.
 */
@Stable
class EmojiPanelState(open: Boolean = false) {
	var isOpen by mutableStateOf(open)
	// the tallest keyboard seen so far: the panel copies it so switching between the two does not resize the screen
	internal var keyboardHeight by mutableStateOf(DEFAULT_PANEL_HEIGHT)

	fun close() {
		isOpen = false
	}

	companion object {
		val Saver = Saver<EmojiPanelState, Boolean>(save = { it.isOpen }, restore = { EmojiPanelState(it) })
	}
}

@Composable
fun rememberEmojiPanelState(): EmojiPanelState {
	val state = rememberSaveable(saver = EmojiPanelState.Saver) { EmojiPanelState() }
	val density = LocalDensity.current
	val ime = WindowInsets.ime
	val navBars = WindowInsets.navigationBars

	LaunchedEffect(state) {
		snapshotFlow { ime.getBottom(density) - navBars.getBottom(density) }
			.collect { with(density) { if (it.toDp() > state.keyboardHeight) state.keyboardHeight = it.toDp() } }
	}
	// the keyboard came up by any other route (tap on the field, reply): it owns the space again
	LaunchedEffect(state) {
		snapshotFlow { ime.getBottom(density) > navBars.getBottom(density) }
			.collect { visible -> if (visible) state.close() }
	}
	BackHandler(enabled = state.isOpen) { state.close() }
	return state
}

/** The emoji icon for a text field: opens the panel in place of the keyboard, and brings the keyboard back. */
@Composable
fun EmojiToggleButton(
	state: EmojiPanelState,
	focusRequester: FocusRequester,
	modifier: Modifier = Modifier,
	colors: IconButtonColors = IconButtonDefaults.iconButtonColors()
) {
	val keyboardController = LocalSoftwareKeyboardController.current
	IconButton(
		modifier = modifier,
		colors = colors,
		onClick = {
			if (state.isOpen) {
				state.close()
				focusRequester.requestFocus()
				keyboardController?.show()
			} else {
				state.isOpen = true
				keyboardController?.hide()
			}
		}
	) {
		Icon(
			imageVector = if (state.isOpen) Icons.Outlined.Keyboard else Icons.Outlined.EmojiEmotions,
			contentDescription = stringResource(if (state.isOpen) R.string.show_keyboard else R.string.show_all_emoji)
		)
	}
}

/** The full emoji list, as tall as the keyboard it replaces. Call only while [EmojiPanelState.isOpen]. */
@Composable
fun EmojiPanel(
	state: EmojiPanelState,
	onEmojiPicked: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	val maxHeight = (LocalConfiguration.current.screenHeightDp * MAX_PANEL_SCREEN_FRACTION).dp
	EmojiPickerPanel(
		onEmojiPicked = onEmojiPicked,
		modifier = modifier.height(state.keyboardHeight.coerceIn(MIN_PANEL_HEIGHT, maxHeight))
	)
}

/** Full emoji list (categories, recents, skin tones) from the Jetpack emoji picker, sized by the caller. */
@Composable
private fun EmojiPickerPanel(
	onEmojiPicked: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	val currentOnPicked by rememberUpdatedState(onEmojiPicked)
	val background = MaterialTheme.colorScheme.surface.toArgb()

	AndroidView(
		modifier = modifier,
		factory = { context ->
			// the app window theme is a light platform theme: the picker takes its text colors from the context theme
			EmojiPickerView(ContextThemeWrapper(context, android.R.style.Theme_Material_NoActionBar)).apply {
				setOnEmojiPickedListener { item -> currentOnPicked(item.emoji) }
			}
		},
		update = { it.setBackgroundColor(background) }
	)
}

/**
 * Puts [emoji] where the cursor is (replacing the selection, if any) and leaves the cursor right after it.
 * Returns null when the result would pass [maxLength] (counted in UTF-16 units, like the server's @Size),
 * so the caller keeps the text as it was instead of cutting an emoji in half.
 */
fun insertEmoji(value: TextFieldValue, emoji: String, maxLength: Int): TextFieldValue? {
	val selection = value.selection
	val updated = value.text.replaceRange(selection.min, selection.max, emoji)
	if (updated.length > maxLength) return null
	val cursor = selection.min + emoji.length
	return TextFieldValue(text = updated, selection = TextRange(cursor))
}
