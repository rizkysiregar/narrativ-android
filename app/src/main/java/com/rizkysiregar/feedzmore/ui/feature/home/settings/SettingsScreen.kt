package com.rizkysiregar.feedzmore.ui.feature.home.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
	viewModel: SettingsViewModel = hiltViewModel(),
	onBackClick: () -> Unit,
	onLoggedOut: () -> Unit
) {
	var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Settings") },
				navigationIcon = {
					IconButton(onClick = onBackClick) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "Back"
						)
					}
				}
			)
		}
	) { padding ->
		Column(modifier = Modifier.fillMaxSize().padding(padding)) {
			ListItem(
				modifier = Modifier.clickable { showLogoutDialog = true },
				headlineContent = { Text("Logout", color = MaterialTheme.colorScheme.error) },
				leadingContent = {
					Icon(
						imageVector = Icons.AutoMirrored.Filled.ExitToApp,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.error
					)
				}
			)
		}
	}

	if (showLogoutDialog) {
		AlertDialog(
			onDismissRequest = { showLogoutDialog = false },
			title = { Text("Logout") },
			text = { Text("Are you sure you want to logout?") },
			confirmButton = {
				TextButton(onClick = {
					showLogoutDialog = false
					viewModel.logout()
					onLoggedOut()
				}) { Text("Logout") }
			},
			dismissButton = {
				TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
			}
		)
	}
}
