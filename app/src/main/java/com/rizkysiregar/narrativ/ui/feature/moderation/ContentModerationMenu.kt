package com.rizkysiregar.narrativ.ui.feature.moderation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizkysiregar.narrativ.domain.moderation.REPORT_DESCRIPTION_MAX_LENGTH
import com.rizkysiregar.narrativ.domain.moderation.ReportReason
import com.rizkysiregar.narrativ.domain.moderation.ReportTarget
import androidx.compose.ui.res.stringResource
import com.rizkysiregar.narrativ.R

/** What a report or block is about. [authorId] is null when the server did not send it (then no menu shows). */
data class ModerationTarget(
	val type: ReportTarget,
	val targetId: String,
	val authorId: String?,
	val authorUsername: String
)

/**
 * The "..." menu of a post, a comment or a profile: report it, or block its author. Nothing is shown for
 * the user's own content. [onBlocked] runs after the author was blocked, so the screen can drop their content.
 */
@Composable
fun ContentModerationMenu(
	target: ModerationTarget,
	modifier: Modifier = Modifier,
	iconSize: Dp = 20.dp,
	onBlocked: () -> Unit = {},
	viewModel: ModerationViewModel = hiltViewModel()
) {
	val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
	val authorId = target.authorId
	if (authorId == null || authorId == currentUserId) return

	val context = LocalContext.current
	var menuOpen by remember { mutableStateOf(false) }
	var showReport by rememberSaveable { mutableStateOf(false) }
	var showBlock by rememberSaveable { mutableStateOf(false) }

	val subject = stringResource(
		when (target.type) {
			ReportTarget.POST -> R.string.subject_post
			ReportTarget.COMMENT -> R.string.subject_comment
			ReportTarget.USER -> R.string.subject_account
		}
	)

	fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

	Column(modifier = modifier) {
		IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(iconSize + 16.dp)) {
			Icon(
				imageVector = Icons.Filled.MoreVert,
				contentDescription = stringResource(R.string.options_for, subject),
				tint = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.size(iconSize)
			)
		}
		DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
			DropdownMenuItem(
				text = { Text(stringResource(R.string.report_subject, subject)) },
				leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
				onClick = {
					menuOpen = false
					showReport = true
				}
			)
			DropdownMenuItem(
				text = { Text(stringResource(R.string.block_user, target.authorUsername)) },
				leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
				onClick = {
					menuOpen = false
					showBlock = true
				}
			)
		}
	}

	if (showReport) {
		ReportDialog(
			subject = subject,
			onDismiss = { showReport = false },
			onSubmit = { reason, description ->
				showReport = false
				viewModel.report(target.type, target.targetId, reason, description) { result ->
					when (result) {
						ModerationResult.Success ->
							toast(context.getString(R.string.report_thanks))
						is ModerationResult.Failure -> result.message?.let(::toast)
					}
				}
			}
		)
	}

	if (showBlock) {
		AlertDialog(
			onDismissRequest = { showBlock = false },
			title = { Text(stringResource(R.string.block_user_title, target.authorUsername)) },
			text = {
				Text(stringResource(R.string.block_body, target.authorUsername))
			},
			confirmButton = {
				TextButton(onClick = {
					showBlock = false
					viewModel.block(authorId) { result ->
						when (result) {
							ModerationResult.Success -> {
								toast(context.getString(R.string.user_blocked, target.authorUsername))
								onBlocked()
							}
							is ModerationResult.Failure -> result.message?.let(::toast)
						}
					}
				}) { Text(stringResource(R.string.block), color = MaterialTheme.colorScheme.error) }
			},
			dismissButton = { TextButton(onClick = { showBlock = false }) { Text(stringResource(R.string.cancel)) } }
		)
	}
}

@Composable
private fun ReportDialog(
	subject: String,
	onDismiss: () -> Unit,
	onSubmit: (ReportReason, String?) -> Unit
) {
	var reasonName by rememberSaveable { mutableStateOf<String?>(null) }
	var description by rememberSaveable { mutableStateOf("") }
	val reason = ReportReason.entries.firstOrNull { it.name == reasonName }
	val canSubmit = reason != null && (!reason.needsDescription || description.isNotBlank())

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(R.string.report_subject, subject)) },
		text = {
			Column(
				modifier = Modifier
					.heightIn(max = 420.dp)
					.verticalScroll(rememberScrollState())
			) {
				Text(
					stringResource(R.string.report_pick_reason),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Spacer(Modifier.height(8.dp))
				ReportReason.entries.forEach { option ->
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.selectable(
								selected = option == reason,
								role = Role.RadioButton,
								onClick = { reasonName = option.name }
							),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(4.dp)
					) {
						RadioButton(selected = option == reason, onClick = null)
						Text(stringResource(option.labelRes), modifier = Modifier.padding(vertical = 10.dp))
					}
				}
				if (reason != null) {
					Spacer(Modifier.height(8.dp))
					OutlinedTextField(
						value = description,
						onValueChange = { description = it.take(REPORT_DESCRIPTION_MAX_LENGTH) },
						label = { Text(stringResource(if (reason.needsDescription) R.string.report_describe_required else R.string.report_describe_optional)) },
						supportingText = { Text("${description.length}/$REPORT_DESCRIPTION_MAX_LENGTH") },
						minLines = 2,
						maxLines = 4,
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		},
		confirmButton = {
			TextButton(
				enabled = canSubmit,
				onClick = { reason?.let { onSubmit(it, description.takeIf(String::isNotBlank)) } }
			) { Text(stringResource(R.string.send_report)) }
		},
		dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
	)
}
