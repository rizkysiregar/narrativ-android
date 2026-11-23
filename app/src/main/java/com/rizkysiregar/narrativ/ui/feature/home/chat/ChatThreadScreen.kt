package com.rizkysiregar.narrativ.ui.feature.home.chat

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rizkysiregar.marpokat.Attachment
import com.rizkysiregar.marpokat.Message
import com.rizkysiregar.marpokat.PendingMessage
import com.rizkysiregar.marpokat.PendingStatus
import com.rizkysiregar.narrativ.R

private val QUICK_REACTIONS = listOf("👍", "❤️", "😂")
private const val LOAD_AHEAD = 6

@Composable
fun ChatThreadRoute(onBackClick: () -> Unit, viewModel: ChatThreadViewModel = hiltViewModel()) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	ChatThreadScreen(
		state = state, onBackClick = onBackClick, onSend = viewModel::send, onTyping = viewModel::onTyping,
		onAttach = viewModel::attach, onRemoveUpload = viewModel::removeUpload, onReact = viewModel::react,
		onReply = viewModel::reply, onEdit = viewModel::startEdit, onDelete = viewModel::delete,
		onCancelMode = viewModel::cancelComposerMode, onRetry = viewModel::retry, onDiscard = viewModel::discard,
		onLoadOlder = viewModel::loadOlder
	)
}

@Composable
private fun ChatThreadScreen(
	state: ChatThreadUiState,
	onBackClick: () -> Unit,
	onSend: (String) -> Unit,
	onTyping: () -> Unit,
	onAttach: (Uri) -> Unit,
	onRemoveUpload: (Int) -> Unit,
	onReact: (Message, String) -> Unit,
	onReply: (Message) -> Unit,
	onEdit: (Message) -> Unit,
	onDelete: (Message) -> Unit,
	onCancelMode: () -> Unit,
	onRetry: (String) -> Unit,
	onDiscard: (String) -> Unit,
	onLoadOlder: () -> Unit
) {
	var deleting by remember { mutableStateOf<Message?>(null) }
	Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding().navigationBarsPadding()) {
		Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
			IconButton(onClick = onBackClick) {
				Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
			}
			Text(state.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
		}
		HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

		when {
			state.unavailable -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
				Text(stringResource(R.string.chat_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
			state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
			else -> Messages(state, Modifier.weight(1f), onReact, onReply, onEdit, { deleting = it }, onRetry, onDiscard, onLoadOlder)
		}
		if (state.typing.isNotEmpty()) {
			Text(
				stringResource(R.string.chat_typing, state.typing.joinToString(", ")),
				style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
			)
		}
		if (!state.unavailable && !state.loading) Composer(state, onSend, onTyping, onAttach, onRemoveUpload, onCancelMode)
	}

	deleting?.let { message ->
		AlertDialog(
			onDismissRequest = { deleting = null },
			title = { Text(stringResource(R.string.chat_delete_title)) },
			text = { Text(stringResource(R.string.chat_delete_body)) },
			confirmButton = { TextButton(onClick = { onDelete(message); deleting = null }) { Text(stringResource(R.string.delete)) } },
			dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } }
		)
	}
	state.problem?.let { /* worded by the route's snackbar host in a later pass; the state is cleared by the next action */ }
}

@Composable
private fun Messages(
	state: ChatThreadUiState,
	modifier: Modifier,
	onReact: (Message, String) -> Unit,
	onReply: (Message) -> Unit,
	onEdit: (Message) -> Unit,
	onDelete: (Message) -> Unit,
	onRetry: (String) -> Unit,
	onDiscard: (String) -> Unit,
	onLoadOlder: () -> Unit
) {
	val listState = rememberLazyListState()
	// newest at the bottom: the list is reversed, so index 0 is the newest and "older" is the end of the list
	val newestFirst = remember(state.messages) { state.messages.asReversed() }
	val pendingNewestFirst = remember(state.pending) { state.pending.asReversed() }
	val newestKey = pendingNewestFirst.firstOrNull()?.clientMsgId ?: newestFirst.firstOrNull()?.id
	LaunchedEffect(newestKey) { if (listState.firstVisibleItemIndex <= 1) listState.animateScrollToItem(0) }
	LaunchedEffect(listState, state.hasOlder, newestFirst.size) {
		androidx.compose.runtime.snapshotFlow {
			val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
			last >= listState.layoutInfo.totalItemsCount - LOAD_AHEAD
		}.collect { nearEnd -> if (nearEnd && state.hasOlder) onLoadOlder() }
	}
	LazyColumn(state = listState, reverseLayout = true, modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
		items(pendingNewestFirst, key = { "p-" + it.clientMsgId }) { PendingBubble(it, onRetry, onDiscard) }
		items(newestFirst, key = { it.id }) { message ->
			Bubble(message, mine = message.sender?.externalId == state.myId, showSender = state.group, frozen = state.frozen, onReact, onReply, onEdit, onDelete)
		}
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(
	message: Message,
	mine: Boolean,
	showSender: Boolean,
	frozen: Boolean,
	onReact: (Message, String) -> Unit,
	onReply: (Message) -> Unit,
	onEdit: (Message) -> Unit,
	onDelete: (Message) -> Unit
) {
	if (message.isSystem) {
		Text(
			message.text.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.fillMaxWidth().padding(8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center
		)
		return
	}
	var menu by remember { mutableStateOf(false) }
	Column(
		modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
		horizontalAlignment = if (mine) Alignment.End else Alignment.Start
	) {
		if (message.isDeleted) {
			Text(
				stringResource(R.string.chat_deleted), fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp)
			)
			return@Column
		}
		Box {
			Column(
				modifier = Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp))
					.background(if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
					.combinedClickable(onClick = {}, onLongClick = { if (!frozen) menu = true })
					.padding(horizontal = 12.dp, vertical = 8.dp)
			) {
				val content = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
				if (showSender && !mine) {
					Text(message.sender?.name.orEmpty().ifBlank { message.sender?.externalId.orEmpty() }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = content)
				}
				message.replyTo?.let { quote ->
					Text(
						if (quote.deleted) stringResource(R.string.chat_deleted) else quote.text ?: stringResource(R.string.chat_file),
						style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.7f), maxLines = 2,
						modifier = Modifier.padding(bottom = 4.dp)
					)
				}
				message.attachments.forEach { AttachmentView(it) }
				message.text?.takeIf { it.isNotBlank() }?.let { Text(it, color = content) }
				if (message.editedAt != null) {
					Text(stringResource(R.string.chat_edited), style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.7f))
				}
			}
			DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
				Row(modifier = Modifier.padding(horizontal = 8.dp)) {
					QUICK_REACTIONS.forEach { emoji ->
						TextButton(onClick = { menu = false; onReact(message, emoji) }) { Text(emoji) }
					}
				}
				DropdownMenuItem(text = { Text(stringResource(R.string.chat_reply)) }, onClick = { menu = false; onReply(message) })
				if (mine && !message.text.isNullOrEmpty()) DropdownMenuItem(text = { Text(stringResource(R.string.chat_edit)) }, onClick = { menu = false; onEdit(message) })
				if (mine) DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; onDelete(message) })
			}
		}
		if (message.reactions.counts.isNotEmpty()) {
			Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
				message.reactions.counts.forEach { (emoji, count) ->
					val on = emoji in message.reactions.mine
					Text(
						"$emoji $count", style = MaterialTheme.typography.labelMedium,
						modifier = Modifier.clip(RoundedCornerShape(12.dp))
							.border(1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
							.clickable(enabled = !frozen) { onReact(message, emoji) }.padding(horizontal = 8.dp, vertical = 2.dp)
					)
				}
			}
		}
	}
}

@Composable
private fun AttachmentView(attachment: Attachment) {
	val context = LocalContext.current
	val open = { attachment.url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } ?: Unit }
	when {
		attachment.status == "failed" -> Text(stringResource(R.string.chat_attachment_failed), fontStyle = FontStyle.Italic)
		attachment.kind == "file" -> Row(
			modifier = Modifier.clickable(enabled = attachment.url != null, onClick = { open() }).padding(vertical = 4.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null)
			Text(attachment.fileName ?: stringResource(R.string.chat_file), modifier = Modifier.padding(start = 8.dp))
		}
		attachment.url == null -> Box( // keeps the room of the picture while its thumbnail is made, so the list does not jump
			modifier = Modifier.widthIn(max = 240.dp).fillMaxWidth().aspectRatio(ratioOf(attachment)).clip(RoundedCornerShape(10.dp))
				.background(MaterialTheme.colorScheme.outlineVariant),
			contentAlignment = Alignment.Center
		) { Text(stringResource(R.string.chat_processing), style = MaterialTheme.typography.labelSmall) }
		else -> Box(
			modifier = Modifier.widthIn(max = 240.dp).fillMaxWidth().aspectRatio(ratioOf(attachment)).clip(RoundedCornerShape(10.dp)).clickable(onClick = { open() })
		) {
			AsyncImage(
				model = attachment.thumbnailUrl ?: attachment.url, contentDescription = stringResource(if (attachment.kind == "video") R.string.chat_video else R.string.chat_photo),
				contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
			)
			if (attachment.kind == "video") {
				Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.align(Alignment.Center), tint = androidx.compose.ui.graphics.Color.White)
			}
		}
	}
}

private fun ratioOf(a: Attachment): Float = if ((a.width ?: 0) > 0 && (a.height ?: 0) > 0) a.width!!.toFloat() / a.height!! else 4f / 3f

@Composable
private fun PendingBubble(pending: PendingMessage, onRetry: (String) -> Unit, onDiscard: (String) -> Unit) {
	Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalAlignment = Alignment.End) {
		Text(
			pending.text.ifBlank { stringResource(R.string.chat_file) },
			color = MaterialTheme.colorScheme.onPrimary,
			modifier = Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 8.dp)
		)
		if (pending.status == PendingStatus.Failed) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(pending.errorMessage ?: stringResource(R.string.chat_send_failed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
				TextButton(onClick = { onRetry(pending.clientMsgId) }) { Text(stringResource(R.string.retry)) }
				TextButton(onClick = { onDiscard(pending.clientMsgId) }) { Text(stringResource(R.string.chat_discard)) }
			}
		} else {
			Text(stringResource(R.string.chat_sending), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
		}
	}
}

@Composable
private fun Composer(
	state: ChatThreadUiState,
	onSend: (String) -> Unit,
	onTyping: () -> Unit,
	onAttach: (Uri) -> Unit,
	onRemoveUpload: (Int) -> Unit,
	onCancelMode: () -> Unit
) {
	if (state.frozen) {
		Text(stringResource(R.string.chat_frozen), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(16.dp))
		return
	}
	var text by remember { mutableStateOf("") }
	// starting an edit puts the text of that message in the box
	LaunchedEffect(state.editing?.id) { state.editing?.let { text = it.text.orEmpty() } }
	val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(onAttach) }

	HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
	val mode = state.editing?.let { stringResource(R.string.chat_editing) }
		?: state.replyTo?.let { stringResource(R.string.chat_replying_to, it.text ?: stringResource(R.string.chat_file)) }
	if (mode != null) {
		Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
			Text(mode, maxLines = 1, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
			IconButton(onClick = { onCancelMode(); if (state.editing != null) text = "" }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel)) }
		}
	}
	state.uploads.forEach { upload ->
		Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
			Text(upload.name, maxLines = 1, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
			LinearProgressIndicator(progress = { upload.progress }, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
			IconButton(onClick = { onRemoveUpload(upload.key) }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.chat_remove_upload)) }
		}
	}
	Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
		if (state.editing == null) {
			IconButton(onClick = { picker.launch("*/*") }) { Icon(Icons.Default.AttachFile, contentDescription = stringResource(R.string.chat_attach)) }
		}
		OutlinedTextField(
			value = text, onValueChange = { text = it; if (state.editing == null) onTyping() },
			placeholder = { Text(stringResource(R.string.chat_message_hint)) },
			modifier = Modifier.weight(1f), maxLines = 5, shape = RoundedCornerShape(24.dp)
		)
		IconButton(
			enabled = state.canSend && (text.isNotBlank() || state.uploads.isNotEmpty()),
			onClick = { onSend(text); text = "" }
		) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send)) }
	}
}
