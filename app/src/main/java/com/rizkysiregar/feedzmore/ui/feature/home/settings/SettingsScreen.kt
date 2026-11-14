package com.rizkysiregar.feedzmore.ui.feature.home.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizkysiregar.feedzmore.ui.feature.legal.LegalDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
	viewModel: SettingsViewModel = hiltViewModel(),
	onBackClick: () -> Unit,
	onBlockedUsersClick: () -> Unit,
	onLegalClick: (LegalDocument) -> Unit,
	onLoggedOut: () -> Unit
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
	var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Pengaturan") },
				navigationIcon = {
					IconButton(onClick = onBackClick) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "Kembali"
						)
					}
				}
			)
		}
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.verticalScroll(rememberScrollState())
		) {
			SettingsItem(
				title = "Akun yang diblokir",
				icon = { Icon(Icons.Outlined.Block, contentDescription = null) },
				showChevron = true,
				onClick = onBlockedUsersClick
			)
			HorizontalDivider()
			SettingsItem(
				title = "Kebijakan Privasi",
				icon = { Icon(Icons.Outlined.PrivacyTip, contentDescription = null) },
				showChevron = true,
				onClick = { onLegalClick(LegalDocument.PRIVACY_POLICY) }
			)
			SettingsItem(
				title = "Syarat & Ketentuan",
				icon = { Icon(Icons.Outlined.Description, contentDescription = null) },
				showChevron = true,
				onClick = { onLegalClick(LegalDocument.TERMS) }
			)
			HorizontalDivider()
			SettingsItem(
				title = "Keluar",
				icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
				danger = true,
				onClick = { showLogoutDialog = true }
			)
			SettingsItem(
				title = "Hapus akun",
				icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null) },
				danger = true,
				onClick = { showDeleteDialog = true }
			)
		}
	}

	if (showLogoutDialog) {
		AlertDialog(
			onDismissRequest = { if (!uiState.isBusy) showLogoutDialog = false },
			title = { Text("Keluar") },
			text = { Text("Yakin ingin keluar dari akun ini?") },
			confirmButton = {
				TextButton(
					enabled = !uiState.isBusy,
					onClick = { viewModel.logout(onDone = onLoggedOut) }
				) { Text(if (uiState.isBusy) "Keluar..." else "Keluar") }
			},
			dismissButton = {
				TextButton(
					enabled = !uiState.isBusy,
					onClick = { showLogoutDialog = false }
				) { Text("Batal") }
			}
		)
	}

	if (showDeleteDialog) {
		DeleteAccountDialog(
			isBusy = uiState.isBusy,
			error = uiState.deleteError,
			onDismiss = {
				showDeleteDialog = false
				viewModel.clearDeleteError()
			},
			onConfirmWithPassword = { password -> viewModel.deleteAccountWithPassword(password, onLoggedOut) },
			onConfirmWithGoogle = { viewModel.deleteAccountWithGoogle(onLoggedOut) }
		)
	}
}

@Composable
private fun SettingsItem(
	title: String,
	icon: @Composable () -> Unit,
	onClick: () -> Unit,
	danger: Boolean = false,
	showChevron: Boolean = false
) {
	val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
	ListItem(
		modifier = Modifier.clickable(onClick = onClick),
		colors = ListItemDefaults.colors(
			headlineColor = color,
			leadingIconColor = color,
			trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
		),
		headlineContent = { Text(title) },
		leadingContent = icon,
		trailingContent = if (showChevron) {
			{ Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) }
		} else null
	)
}

/**
 * Deleting is final, so the dialog says what goes away and asks for proof it is really the owner:
 * the password, or the Google account for someone who signed up with Google and has no password.
 */
@Composable
private fun DeleteAccountDialog(
	isBusy: Boolean,
	error: String?,
	onDismiss: () -> Unit,
	onConfirmWithPassword: (String) -> Unit,
	onConfirmWithGoogle: () -> Unit
) {
	var password by rememberSaveable { mutableStateOf("") }

	AlertDialog(
		onDismissRequest = { if (!isBusy) onDismiss() },
		title = { Text("Hapus akun?") },
		text = {
			Column {
				Text(
					"Akun, postingan, dan data pribadi Anda akan dihapus permanen dan tidak bisa dikembalikan. " +
						"Komentar Anda akan dikosongkan, dan Anda keluar dari semua perangkat."
				)
				Spacer(Modifier.height(16.dp))
				OutlinedTextField(
					value = password,
					onValueChange = { password = it },
					label = { Text("Kata sandi") },
					singleLine = true,
					enabled = !isBusy,
					visualTransformation = PasswordVisualTransformation(),
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
					isError = error != null,
					supportingText = error?.let { { Text(it) } },
					modifier = Modifier.fillMaxWidth()
				)
				Spacer(Modifier.height(8.dp))
				Text(
					"Masuk dengan Google dan tidak punya kata sandi?",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Start
				)
				OutlinedButton(
					enabled = !isBusy,
					onClick = onConfirmWithGoogle,
					modifier = Modifier.fillMaxWidth()
				) { Text("Verifikasi dengan Google") }
			}
		},
		confirmButton = {
			Button(
				enabled = !isBusy && password.isNotBlank(),
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.error,
					contentColor = MaterialTheme.colorScheme.onError
				),
				onClick = { onConfirmWithPassword(password) }
			) { Text(if (isBusy) "Menghapus..." else "Hapus akun") }
		},
		dismissButton = {
			TextButton(enabled = !isBusy, onClick = onDismiss) { Text("Batal") }
		}
	)
}
