package com.rizkysiregar.feedzmore.ui.feature.moderation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizkysiregar.feedzmore.ui.components.ProfileAvatar
import com.rizkysiregar.feedzmore.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersRoute(
	onBackClick: () -> Unit,
	viewModel: BlockedUsersViewModel = hiltViewModel()
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.blocked_accounts)) },
				navigationIcon = {
					IconButton(onClick = onBackClick) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
					}
				}
			)
		}
	) { padding ->
		Box(modifier = Modifier.fillMaxSize().padding(padding)) {
			when {
				state.isLoading && state.users.isEmpty() ->
					CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

				state.loadFailed -> Column(
					modifier = Modifier.align(Alignment.Center).padding(24.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Text(stringResource(R.string.error_load_list), color = MaterialTheme.colorScheme.onSurfaceVariant)
					TextButton(onClick = viewModel::load) { Text(stringResource(R.string.try_again)) }
				}

				state.users.isEmpty() -> Text(
					text = stringResource(R.string.no_blocked_users),
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Center,
					modifier = Modifier.align(Alignment.Center).padding(24.dp)
				)

				else -> LazyColumn {
					items(state.users, key = { it.id }) { user ->
						ListItem(
							headlineContent = { Text("@${user.username}") },
							leadingContent = {
								ProfileAvatar(
									imageUrl = user.profileImage,
									userId = "",
									userName = user.username,
									size = 44.dp
								)
							},
							trailingContent = {
								OutlinedButton(
									enabled = user.id !in state.unblocking,
									onClick = { viewModel.unblock(user) }
								) { Text(stringResource(R.string.unblock)) }
							},
							colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
						)
					}
				}
			}
		}
	}
}
