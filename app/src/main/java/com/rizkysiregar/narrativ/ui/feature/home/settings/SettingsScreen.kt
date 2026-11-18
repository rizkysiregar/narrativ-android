package com.rizkysiregar.narrativ.ui.feature.home.settings

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
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
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
import androidx.compose.material3.Switch
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
import com.rizkysiregar.narrativ.ui.feature.legal.LegalDocument
import com.rizkysiregar.narrativ.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.RadioButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import com.rizkysiregar.narrativ.i18n.AppLanguage
import com.rizkysiregar.narrativ.i18n.AppLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
	viewModel: SettingsViewModel = hiltViewModel(),
	onBackClick: () -> Unit,
	onBlockedUsersClick: () -> Unit,
	onChangeEmailClick: () -> Unit,
	onLegalClick: (LegalDocument) -> Unit,
	onLoggedOut: () -> Unit
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val telemetryEnabled by viewModel.telemetryEnabled.collectAsStateWithLifecycle()
	var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
	var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
	var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.settings)) },
				navigationIcon = {
					IconButton(onClick = onBackClick) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
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
				title = stringResource(R.string.change_email),
				icon = { Icon(Icons.Outlined.Email, contentDescription = null) },
				showChevron = true,
				onClick = onChangeEmailClick
			)
			HorizontalDivider()
			SettingsItem(
				title = stringResource(R.string.blocked_accounts),
				icon = { Icon(Icons.Outlined.Block, contentDescription = null) },
				showChevron = true,
				onClick = onBlockedUsersClick
			)
			HorizontalDivider()
			SettingsItem(
				title = stringResource(R.string.settings_language),
				icon = { Icon(Icons.Outlined.Language, contentDescription = null) },
				showChevron = true,
				onClick = { showLanguageDialog = true }
			)
			HorizontalDivider()
			SettingsItem(
				title = stringResource(R.string.privacy_policy),
				icon = { Icon(Icons.Outlined.PrivacyTip, contentDescription = null) },
				showChevron = true,
				onClick = { onLegalClick(LegalDocument.PRIVACY_POLICY) }
			)
			SettingsItem(
				title = stringResource(R.string.terms),
				icon = { Icon(Icons.Outlined.Description, contentDescription = null) },
				showChevron = true,
				onClick = { onLegalClick(LegalDocument.TERMS) }
			)
			HorizontalDivider()
			ListItem(
				modifier = Modifier.clickable { viewModel.setTelemetryEnabled(!telemetryEnabled) },
				headlineContent = { Text(stringResource(R.string.share_usage_data)) },
				supportingContent = {
					Text(stringResource(R.string.share_usage_data_hint))
				},
				leadingContent = { Icon(Icons.Outlined.Analytics, contentDescription = null) },
				trailingContent = {
					Switch(checked = telemetryEnabled, onCheckedChange = viewModel::setTelemetryEnabled)
				}
			)
			HorizontalDivider()
			SettingsItem(
				title = stringResource(R.string.log_out),
				icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
				danger = true,
				onClick = { showLogoutDialog = true }
			)
			SettingsItem(
				title = stringResource(R.string.delete_account),
				icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null) },
				danger = true,
				onClick = { showDeleteDialog = true }
			)
		}
	}

	if (showLanguageDialog) {
		LanguageDialog(
			selected = AppLocale.selected(),
			onSelect = { language ->
				showLanguageDialog = false
				viewModel.setLanguage(language)
			},
			onDismiss = { showLanguageDialog = false }
		)
	}

	if (showLogoutDialog) {
		AlertDialog(
			onDismissRequest = { if (!uiState.isBusy) showLogoutDialog = false },
			title = { Text(stringResource(R.string.log_out)) },
			text = { Text(stringResource(R.string.log_out_confirm)) },
			confirmButton = {
				TextButton(
					enabled = !uiState.isBusy,
					onClick = { viewModel.logout(onDone = onLoggedOut) }
				) { Text(if (uiState.isBusy) stringResource(R.string.logging_out) else stringResource(R.string.log_out)) }
			},
			dismissButton = {
				TextButton(
					enabled = !uiState.isBusy,
					onClick = { showLogoutDialog = false }
				) { Text(stringResource(R.string.cancel)) }
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

/** System default, English or Indonesian. The language names are shown in their own language on purpose. */
@Composable
private fun LanguageDialog(selected: AppLanguage?, onSelect: (AppLanguage?) -> Unit, onDismiss: () -> Unit) {
	val options = listOf(
		null to stringResource(R.string.language_system),
		AppLanguage.ENGLISH to "English",
		AppLanguage.INDONESIAN to "Bahasa Indonesia"
	)
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(R.string.settings_language)) },
		text = {
			Column {
				options.forEach { (language, label) ->
					Row(
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier
							.fillMaxWidth()
							.selectable(selected = language == selected, role = Role.RadioButton, onClick = { onSelect(language) })
							.padding(vertical = 4.dp)
					) {
						RadioButton(selected = language == selected, onClick = null)
						Spacer(Modifier.width(12.dp))
						Text(label)
					}
				}
			}
		},
		confirmButton = {},
		dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
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
		title = { Text(stringResource(R.string.delete_account_title)) },
		text = {
			Column {
				Text(
					stringResource(R.string.delete_account_body)
				)
				Spacer(Modifier.height(16.dp))
				OutlinedTextField(
					value = password,
					onValueChange = { password = it },
					label = { Text(stringResource(R.string.password)) },
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
					stringResource(R.string.google_no_password),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Start
				)
				OutlinedButton(
					enabled = !isBusy,
					onClick = onConfirmWithGoogle,
					modifier = Modifier.fillMaxWidth()
				) { Text(stringResource(R.string.verify_with_google)) }
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
			) { Text(if (isBusy) stringResource(R.string.deleting) else stringResource(R.string.delete_account)) }
		},
		dismissButton = {
			TextButton(enabled = !isBusy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
		}
	)
}
