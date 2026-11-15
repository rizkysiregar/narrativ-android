package com.rizkysiregar.feedzmore.ui.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme
import com.rizkysiregar.feedzmore.R
import androidx.compose.ui.res.stringResource

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 16/11/25
 * Description: Software Engineer
 */

/**
 * One failed request can reach the user twice: the global [com.rizkysiregar.feedzmore.data.remote.ApiErrorManager]
 * dialog of MainRoot and the dialog of the screen that made the call. Every [ErrorDialog] registers here, only the
 * first one is drawn, and dismissing it dismisses every dialog that carries the same message, so one tap clears it.
 */
internal class ErrorDialogCoordinator {

	class Entry(val message: String, val onDismiss: () -> Unit)

	private val entries = mutableStateListOf<Entry>()

	val leader: Entry? get() = entries.firstOrNull()

	fun register(entry: Entry) {
		entries.add(entry)
	}

	fun unregister(entry: Entry) {
		entries.remove(entry)
	}

	fun dismissLeader() {
		val message = leader?.message ?: return
		val sameMessage = entries.filter { it.message == message }
		// out of the list first: an owner that does not clear its own state must not keep the dialog on screen
		entries.removeAll(sameMessage)
		sameMessage.forEach { it.onDismiss() }
	}
}

internal val LocalErrorDialogCoordinator = staticCompositionLocalOf { ErrorDialogCoordinator() }

@Composable
fun ErrorDialog(
	message: String,
	onDismiss: () -> Unit
) {
	val coordinator = LocalErrorDialogCoordinator.current
	val latestOnDismiss by rememberUpdatedState(onDismiss)
	val entry = remember(message) { ErrorDialogCoordinator.Entry(message) { latestOnDismiss() } }

	DisposableEffect(coordinator, entry) {
		coordinator.register(entry)
		onDispose { coordinator.unregister(entry) }
	}

	if (coordinator.leader === entry) {
		ErrorDialogContent(message = message, onDismiss = coordinator::dismissLeader)
	}
}

@Composable
private fun ErrorDialogContent(
	message: String,
	onDismiss: () -> Unit
) {
	Dialog(onDismissRequest = onDismiss) {
		Surface(
			shape = RoundedCornerShape(24.dp),
			tonalElevation = 3.dp,
			color = MaterialTheme.colorScheme.surface
		) {
			Column(
				modifier = Modifier
					.padding(24.dp)
					.widthIn(min = 260.dp, max = 320.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Box(
					modifier = Modifier
						.size(56.dp)
						.background(MaterialTheme.colorScheme.errorContainer, CircleShape),
					contentAlignment = Alignment.Center
				) {
					Icon(
						imageVector = Icons.Outlined.ErrorOutline,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onErrorContainer,
						modifier = Modifier.size(28.dp)
					)
				}

				Spacer(modifier = Modifier.height(16.dp))

				Text(
					text = stringResource(R.string.oops),
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold
				)

				Spacer(modifier = Modifier.height(8.dp))

				// scrolls instead of being cut off: the message is the one thing the user has to get
				Text(
					text = message,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Center,
					modifier = Modifier
						.heightIn(max = 220.dp)
						.verticalScroll(rememberScrollState())
				)

				Spacer(modifier = Modifier.height(24.dp))

				Button(
					onClick = onDismiss,
					modifier = Modifier
						.fillMaxWidth()
						.height(48.dp),
					shape = RoundedCornerShape(12.dp)
				) {
					Text("OK")
				}
			}
		}
	}
}

@Composable
@Preview(showBackground = true)
fun ErrorDialogPreview() {
	FeedzmoreTheme {
		ErrorDialog(
			message = "This username is already taken. Try another one!",
			onDismiss = {}
		)
	}
}
