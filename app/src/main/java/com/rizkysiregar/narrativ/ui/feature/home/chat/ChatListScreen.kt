package com.rizkysiregar.narrativ.ui.feature.home.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.ui.components.ProfileAvatar
import com.rizkysiregar.narrativ.util.formatTimeAgo

@Composable
fun ChatListRoute(
	onBackClick: () -> Unit,
	onOpenChat: (channelId: String) -> Unit,
	viewModel: ChatListViewModel = hiltViewModel()
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	ChatListScreen(state, onBackClick, onOpenChat, viewModel::retry)
}

@Composable
private fun ChatListScreen(
	state: ChatListUiState,
	onBackClick: () -> Unit,
	onOpenChat: (String) -> Unit,
	onRetry: () -> Unit
) {
	Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
		Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
			IconButton(onClick = onBackClick) {
				Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
			}
			Text(stringResource(R.string.chat_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
		}
		HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

		when (state) {
			ChatListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
			ChatListUiState.Unavailable -> Message(stringResource(R.string.chat_unavailable_title), stringResource(R.string.chat_unavailable), onRetry)
			is ChatListUiState.Ready -> if (state.rows.isEmpty()) {
				Message(stringResource(R.string.chat_empty_title), stringResource(R.string.chat_empty), null)
			} else {
				Column {
					if (state.offline) {
						Text(
							stringResource(R.string.chat_offline),
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							modifier = Modifier.fillMaxWidth().padding(8.dp)
						)
					}
					LazyColumn(modifier = Modifier.fillMaxSize()) {
						items(state.rows, key = { it.channelId }) { row -> ChatRowItem(row, onClick = { onOpenChat(row.channelId) }) }
					}
				}
			}
		}
	}
}

@Composable
private fun Message(title: String, body: String, onRetry: (() -> Unit)?) {
	Column(
		modifier = Modifier.fillMaxSize().padding(32.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
		Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
		if (onRetry != null) {
			Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(R.string.retry)) }
		}
	}
}

@Composable
private fun ChatRowItem(row: ChatRow, onClick: () -> Unit) {
	val online = stringResource(R.string.chat_online)
	Row(
		modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box {
			ProfileAvatar(imageUrl = row.avatarUrl, userId = row.peerId, userName = row.title, size = 48.dp)
			if (row.online) {
				Box(
					modifier = Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape)
						.background(MaterialTheme.colorScheme.primary).semantics { contentDescription = online }
				)
			}
		}
		Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
			Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
				Text(
					row.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
					style = MaterialTheme.typography.bodyLarge,
					fontWeight = if (row.unread > 0) FontWeight.Bold else FontWeight.Normal,
					modifier = Modifier.weight(1f)
				)
				Text(formatTimeAgo(row.lastAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
			Text(
				previewText(row), maxLines = 1, overflow = TextOverflow.Ellipsis,
				style = MaterialTheme.typography.bodyMedium,
				color = if (row.unread > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		if (row.unread > 0) Badge { Text(if (row.unread > 99) "99+" else row.unread.toString()) }
	}
}

@Composable
private fun previewText(row: ChatRow): String = when {
	row.lastDeleted -> stringResource(R.string.chat_deleted)
	row.lastText != null -> row.lastText
	row.lastAttachmentKind == "image" -> stringResource(R.string.chat_photo)
	row.lastAttachmentKind == "video" -> stringResource(R.string.chat_video)
	row.lastAttachmentKind == "file" -> stringResource(R.string.chat_file)
	else -> stringResource(R.string.chat_no_messages)
}
