package com.rizkysiregar.feedzmore.ui.feature.home.settings

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.user.DeleteAccountRequest
import com.rizkysiregar.feedzmore.data.repository.UserRepository
import com.rizkysiregar.feedzmore.data.session.GoogleIdTokenProvider
import com.rizkysiregar.feedzmore.data.session.RemoteSessionCleaner
import com.rizkysiregar.feedzmore.data.session.SessionManager
import com.rizkysiregar.feedzmore.telemetry.TelemetrySettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import com.rizkysiregar.feedzmore.i18n.AppLanguage
import com.rizkysiregar.feedzmore.i18n.AppLocale
import com.rizkysiregar.feedzmore.i18n.AppStrings
import com.rizkysiregar.feedzmore.data.remote.DeviceSyncScheduler
import com.rizkysiregar.feedzmore.R

data class SettingsUiState(
	/** A logout or an account deletion is running; the buttons wait. */
	val isBusy: Boolean = false,
	/** Why deleting the account failed, when the global error dialog did not already say so. */
	val deleteError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
	private val sessionManager: SessionManager,
	private val remoteSessionCleaner: RemoteSessionCleaner,
	private val userRepository: UserRepository,
	private val googleIdTokenProvider: GoogleIdTokenProvider,
	private val telemetrySettings: TelemetrySettings,
	private val appStrings: AppStrings,
	private val deviceSyncScheduler: DeviceSyncScheduler
) : ViewModel() {

	private val _uiState = MutableStateFlow(SettingsUiState())
	val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

	/** Whether usage data and crash reports are shared; on until the user turns it off. */
	val telemetryEnabled: StateFlow<Boolean> = telemetrySettings.enabled

	fun setTelemetryEnabled(enabled: Boolean) = telemetrySettings.setEnabled(enabled)

	/** null follows the device. The server learns the new language with the next device sync. */
	fun setLanguage(language: AppLanguage?) {
		AppLocale.select(language)
		deviceSyncScheduler.schedule()
	}

	/** Ends the session on the server first (best effort), then clears it on this device. */
	fun logout(onDone: () -> Unit) {
		if (_uiState.value.isBusy) return
		_uiState.update { it.copy(isBusy = true) }
		viewModelScope.launch {
			remoteSessionCleaner.signOut()
			sessionManager.logout()
			onDone()
		}
	}

	fun deleteAccountWithPassword(password: String, onDone: () -> Unit) {
		if (password.isBlank()) {
			_uiState.update { it.copy(deleteError = appStrings.get(R.string.error_password_required)) }
			return
		}
		deleteAccount(onDone) { DeleteAccountRequest(password = password) }
	}

	/** For an account that signs in with Google: the user picks the account again as proof. */
	fun deleteAccountWithGoogle(onDone: () -> Unit) {
		deleteAccount(onDone) { DeleteAccountRequest(idToken = googleIdTokenProvider.get()) }
	}

	fun clearDeleteError() {
		_uiState.update { it.copy(deleteError = null) }
	}

	private fun deleteAccount(onDone: () -> Unit, request: suspend () -> DeleteAccountRequest) {
		if (_uiState.value.isBusy) return
		_uiState.update { it.copy(isBusy = true, deleteError = null) }
		viewModelScope.launch {
			try {
				var deleted = false
				userRepository.deleteAccount(request()).collect { result ->
					when (result) {
						is ResultState.Success -> deleted = true
						is ResultState.Failure -> {
							// HTTP and network errors already reached the user through the global error dialog
							val alreadyShown = result.throwable is HttpException || result.throwable is IOException
							_uiState.update { it.copy(deleteError = result.message.takeUnless { alreadyShown }) }
						}
						else -> Unit
					}
				}
				if (deleted) {
					// the server revoked every session, so there is nothing left to sign out of there
					remoteSessionCleaner.dropPushToken()
					sessionManager.logout()
					onDone()
				}
			} catch (e: CancellationException) {
				throw e
			} catch (e: GetCredentialCancellationException) {
				// the user closed the Google account picker
			} catch (e: GetCredentialException) {
				_uiState.update { it.copy(deleteError = appStrings.get(R.string.error_google_verify)) }
			} finally {
				_uiState.update { it.copy(isBusy = false) }
			}
		}
	}
}
