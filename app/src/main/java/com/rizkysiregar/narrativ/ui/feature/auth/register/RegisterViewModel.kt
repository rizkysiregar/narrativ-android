package com.rizkysiregar.narrativ.ui.feature.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.auth.RegisterRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.RegisterVerifyOtpRequest
import com.rizkysiregar.narrativ.domain.auth.RegisterUseCase
import com.rizkysiregar.narrativ.domain.auth.SendRegisterOtpUseCase
import com.rizkysiregar.narrativ.domain.auth.VerifyRegisterOtpUseCase
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.telemetry.AppAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 */

enum class RegisterStep { FORM, OTP_VERIFICATION }

/** Where the registration OTP went; [sentCount] changes with every (re)send so the resend countdown restarts. */
data class RegisterOtpDelivery(
	val otpSendToEmailAddress: String,
	val expirationTimeInMinute: String,
	val sentCount: Int
)

/**
 * Registration is: form -> (OTP sent to the email -> OTP verified) -> register. The OTP part only exists where the
 * server verifies emails; when it does not (development) the form goes straight to register.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
	private val sendRegisterOtpUseCase: SendRegisterOtpUseCase,
	private val verifyRegisterOtpUseCase: VerifyRegisterOtpUseCase,
	private val registerUseCase: RegisterUseCase,
	private val analytics: AppAnalytics,
	private val clock: Clock
) : ViewModel() {

	private val _step = MutableStateFlow(RegisterStep.FORM)
	val step: StateFlow<RegisterStep> = _step.asStateFlow()

	// loading and failures of every call; Success means the account was created
	private val _state = MutableStateFlow<ResultState<String>>(ResultState.Idle)
	val state: StateFlow<ResultState<String>> = _state.asStateFlow()

	private val _otpDelivery = MutableStateFlow<RegisterOtpDelivery?>(null)
	val otpDelivery: StateFlow<RegisterOtpDelivery?> = _otpDelivery.asStateFlow()

	/** What the user filled in, so the form is still filled when they come back from the OTP step. */
	var form = RegisterState()
		private set

	// A verified email stays valid on the server for a few minutes. Kept so that fixing a taken username does not
	// ask for a new OTP (which the server would refuse inside its resend cooldown).
	private var verifiedEmail: String? = null
	private var registrationToken: String? = null
	private var verifiedAtMillis = 0L

	fun submit(newForm: RegisterState) {
		form = newForm
		val token = validRegistrationTokenFor(newForm.email)
		if (token != null) {
			register(token)
		} else {
			sendOtp()
		}
	}

	fun resendOtp() = sendOtp()

	fun verifyOtp(otp: String) {
		viewModelScope.launch {
			verifyRegisterOtpUseCase(RegisterVerifyOtpRequest(email = form.email, otp = otp))
				.collect { result ->
					when (result) {
						is ResultState.Success -> {
							analytics.log(AnalyticsEvents.REGISTER_OTP_VERIFIED)
							verifiedEmail = form.email
							registrationToken = result.data.registrationToken
							verifiedAtMillis = clock.millis()
							register(result.data.registrationToken)
						}

						is ResultState.Failure -> {
							analytics.log(AnalyticsEvents.REGISTER_OTP_FAILED)
							_state.value = result
						}

						else -> _state.value = ResultState.Loading
					}
				}
		}
	}

	fun backToForm() {
		_step.value = RegisterStep.FORM
		_state.value = ResultState.Idle
	}

	fun resetState() {
		_state.value = ResultState.Idle
	}

	private fun sendOtp() {
		viewModelScope.launch {
			sendRegisterOtpUseCase(form.email).collect { result ->
				when (result) {
					is ResultState.Success -> {
						val delivery = result.data
						if (!delivery.verificationRequired) {
							register(registrationToken = null)
						} else {
							analytics.log(AnalyticsEvents.REGISTER_OTP_SENT)
							_otpDelivery.value = RegisterOtpDelivery(
								otpSendToEmailAddress = delivery.otpSendToEmailAddress.orEmpty(),
								expirationTimeInMinute = delivery.expirationTimeInMinute.orEmpty(),
								sentCount = (_otpDelivery.value?.sentCount ?: 0) + 1
							)
							_step.value = RegisterStep.OTP_VERIFICATION
							_state.value = ResultState.Idle
						}
					}

					is ResultState.Failure -> _state.value = result
					else -> _state.value = ResultState.Loading
				}
			}
		}
	}

	private fun register(registrationToken: String?) {
		viewModelScope.launch {
			registerUseCase(
				RegisterRequest(
					username = form.username,
					password = form.password,
					email = form.email,
					firstName = form.firstName,
					lastName = form.lastName,
					registrationToken = registrationToken
				)
			).collect { result ->
				if (result is ResultState.Success) {
					analytics.log(AnalyticsEvents.SIGN_UP, AnalyticsEvents.Param.METHOD to "email")
				} else if (result is ResultState.Failure) {
					analytics.log(AnalyticsEvents.SIGN_UP_FAILED, AnalyticsEvents.Param.METHOD to "email")
					// e.g. the username is taken: back to the form to fix it, the verified email is kept
					_step.value = RegisterStep.FORM
				}
				_state.value = result
			}
		}
	}

	private fun validRegistrationTokenFor(email: String): String? {
		val ageMillis = clock.millis() - verifiedAtMillis
		return registrationToken.takeIf { verifiedEmail == email && ageMillis < TOKEN_VALID_MILLIS }
	}

	private companion object {
		// the server keeps the token for 10 minutes, stop using it a little earlier
		const val TOKEN_VALID_MILLIS = 9 * 60 * 1000L
	}
}
