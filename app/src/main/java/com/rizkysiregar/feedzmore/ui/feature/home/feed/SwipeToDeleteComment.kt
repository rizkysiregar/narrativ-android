package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 11/11/26
 * Description: Swipe a comment to the left to ask for its deletion. The row always springs back: the
 * caller confirms with the user and removes it from the list once the server agrees. TalkBack users get
 * the same thing as a custom action, since a swipe gesture is not reachable for them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeToDeleteComment(
	onDeleteRequest: () -> Unit,
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit
) {
	val latestRequest by rememberUpdatedState(onDeleteRequest)
	val state = rememberSwipeToDismissBoxState(
		confirmValueChange = { value ->
			if (value == SwipeToDismissBoxValue.EndToStart) latestRequest()
			false
		}
	)

	SwipeToDismissBox(
		state = state,
		modifier = modifier.semantics {
			customActions = listOf(CustomAccessibilityAction("Hapus komentar") { latestRequest(); true })
		},
		enableDismissFromStartToEnd = false,
		backgroundContent = {
			// the row itself has no background of its own (it inherits the sheet's colour), so the red
			// is drawn only in the strip the row has moved away from
			val revealedPx = if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
				-(runCatching { state.requireOffset() }.getOrDefault(0f))
			} else 0f
			if (revealedPx > 0f) {
				Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
					Box(
						modifier = Modifier
							.width(with(LocalDensity.current) { revealedPx.toDp() })
							.fillMaxHeight()
							.background(MaterialTheme.colorScheme.errorContainer),
						contentAlignment = Alignment.CenterEnd
					) {
						Icon(
							imageVector = Icons.Default.Delete,
							contentDescription = "Hapus komentar",
							tint = MaterialTheme.colorScheme.onErrorContainer,
							modifier = Modifier.padding(end = 20.dp)
						)
					}
				}
			}
		}
	) {
		content()
	}
}
