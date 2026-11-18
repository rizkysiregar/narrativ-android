package com.rizkysiregar.narrativ.ui.feature.home.settings

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.cache.JwtTokenHolder
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.user.ConfirmEmailChangeRequest
import com.rizkysiregar.narrativ.data.repository.UserRepository
import com.rizkysiregar.narrativ.data.session.GoogleIdTokenProvider
import com.rizkysiregar.narrativ.i18n.AppStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

data class ChangeEmailUiState(
	val step: Step = Step.ENTER_EMAIL,
	/** The new address as the server masked it, shown above the code field. */
	val sentTo: String? = null,
	val isBusy: Boolean = false,
	/** A mistake the global error dialog did not already report (a malformed email, a missing code, ...). */
	val error: String? = null,
	/** The email was changed and the new session is stored; the screen closes. */
	val done: Boolean = false
) {
	enum class Step { ENTER_EMAIL, ENTER_CODE }
}

/**
 * Changes the email of the account in two steps, like the server: an OTP is sent to the new address, then the code is
 * confirmed together with the password (or the Google account for someone without a password). The server ends every
 * other session and answers a new token pair, which replaces the stored one so this device stays signed in.
 */
@HiltViewModel
class ChangeEmailViewModel @Inject constructor(
	private val userRepository: UserRepository,
	private val googleIdTokenProvider: GoogleIdTokenProvider,
	private val dataStore: DataStoreManager,
	private val tokenHolder: JwtTokenHolder,
	private val appStrings: AppStrings
) : ViewModel() {

	private val _uiState = MutableStateFlow(ChangeEmailUiState())
	val uiState: StateFlow<ChangeEmailUiState> = _uiState.asStateFlow()

	/** The address the current code was sent to; the code only works for it. */
	private var pendingEmail: String = ""

	fun sendCode(newEmail: String) {
		val email = newEmail.trim().lowercase(Locale.ROOT)
		if (!EMAIL.matches(email)) {
			_uiState.update { it.copy(error = appStrings.get(R.string.error_invalid_email)) }
			return
		}
		launchBusy {
			var sentTo: String? = null
			userRepository.sendEmailChangeOtp(email).collect { result ->
				when (result) {
					is ResultState.Success -> sentTo = result.data.otpSendToEmailAddress
					is ResultState.Failure -> reportFailure(result)
					else -> Unit
				}
			}
			sentTo?.let { masked ->
				pendingEmail = email
				_uiState.update { it.copy(step = ChangeEmailUiState.Step.ENTER_CODE, sentTo = masked) }
			}
		}
	}

	fun resendCode() {
		if (pendingEmail.isNotEmpty()) sendCode(pendingEmail)
	}

	/** Back to the address, e.g. after a typo in it. */
	fun editEmail() {
		_uiState.update { it.copy(step = ChangeEmailUiState.Step.ENTER_EMAIL, sentTo = null, error = null) }
	}

	fun confirmWithPassword(otp: String, password: String) {
		if (password.isBlank()) {
			_uiState.update { it.copy(error = appStrings.get(R.string.error_password_required)) }
			return
		}
		confirm(otp) { ConfirmEmailChangeRequest(email = pendingEmail, otp = otp, password = password) }
	}

	/** For an account that signs in with Google: the user picks the account again as proof. */
	fun confirmWithGoogle(otp: String) {
		confirm(otp) { ConfirmEmailChangeRequest(email = pendingEmail, otp = otp, idToken = googleIdTokenProvider.get()) }
	}

	fun clearError() {
		_uiState.update { it.copy(error = null) }
	}

	private fun confirm(otp: String, request: suspend () -> ConfirmEmailChangeRequest) {
		if (!OTP.matches(otp)) {
			_uiState.update { it.copy(error = appStrings.get(R.string.error_otp_incomplete)) }
			return
		}
		launchBusy {
			userRepository.confirmEmailChange(request()).collect { result ->
				when (result) {
					is ResultState.Success -> {
						// the old refresh token was revoked with every other session: only the new pair works now
						dataStore.put(PrefKeys.token, result.data.token)
						dataStore.put(PrefKeys.refreshToken, result.data.refreshToken)
						tokenHolder.setToken(result.data.token)
						_uiState.update { it.copy(done = true) }
					}
					is ResultState.Failure -> reportFailure(result)
					else -> Unit
				}
			}
		}
	}

	private fun launchBusy(block: suspend () -> Unit) {
		if (_uiState.value.isBusy) return
		_uiState.update { it.copy(isBusy = true, error = null) }
		viewModelScope.launch {
			try {
				block()
			} catch (e: CancellationException) {
				throw e
			} catch (e: GetCredentialCancellationException) {
				// the user closed the Google account picker
			} catch (e: GetCredentialException) {
				_uiState.update { it.copy(error = appStrings.get(R.string.error_google_verify)) }
			} finally {
				_uiState.update { it.copy(isBusy = false) }
			}
		}
	}

	// HTTP and network errors already reached the user through the global error dialog
	private fun reportFailure(result: ResultState.Failure) {
		val alreadyShown = result.throwable is HttpException || result.throwable is IOException
		_uiState.update { it.copy(error = result.message.takeUnless { alreadyShown }) }
	}

	private companion object {
		val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
		val OTP = Regex("^\\d{6}$")
	}
}
