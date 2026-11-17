package com.rizkysiregar.narrativ.ui.feature.auth.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.auth.ResetNewPasswordRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.ResetPassVerifyOtpRequest
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ForgotPasswordResponse
import com.rizkysiregar.narrativ.domain.auth.ResetNewPasswordUseCase
import com.rizkysiregar.narrativ.domain.auth.SendResetOtpUseCase
import com.rizkysiregar.narrativ.domain.auth.VerifyOtpResetPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 03/03/26
 * Description: Software Engineer
 */

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
	private val sendResetOtpUseCase: SendResetOtpUseCase,
	private val verifyOtpResetPasswordUseCase: VerifyOtpResetPasswordUseCase,
	private val resetNewPasswordUseCase: ResetNewPasswordUseCase
) : ViewModel() {

	// 1. Current Step State
	private val _currentStep = MutableStateFlow(ResetPasswordStep.EMAIL_ENTRY)
	val currentStep = _currentStep.asStateFlow()


	// 2. Network/API State (Uses your ResultState)
	private val _resetState = MutableStateFlow<ResultState<Any>>(ResultState.Idle)
	val resetState = _resetState.asStateFlow()
	private val _otpDelivery = MutableStateFlow<ForgotPasswordResponse?>(null)
	val otpDelivery = _otpDelivery.asStateFlow()


	// 3. Persistent Data (Needed across steps)
	private var savedEmail: String = ""
	private var savedResetToken: String = "" // Usually returned after OTP is correct

	fun sendOtp(email: String) {
		savedEmail = email
		_otpDelivery.value = null
		viewModelScope.launch {
			_resetState.value = ResultState.Loading
			sendResetOtpUseCase(email)
				.collect { result ->
					if (result is ResultState.Success) {
						_otpDelivery.value = result.data
						_currentStep.value = ResetPasswordStep.OTP_VERIFICATION
					}
					_resetState.value = result
				}
		}
	}

	fun verifyOtp(request: ResetPassVerifyOtpRequest) {
		viewModelScope.launch {
			_resetState.value = ResultState.Loading
			verifyOtpResetPasswordUseCase(request)
				.collect { result ->
					if (result is ResultState.Success) {
						savedResetToken = result.data.resetToken
						_currentStep.value = ResetPasswordStep.NEW_PASSWORD_INPUT
					}
					_resetState.value = result
				}
		}
	}

	fun resetNewPassword(request: ResetNewPasswordRequest) {
		viewModelScope.launch {
			_resetState.value = ResultState.Loading
			resetNewPasswordUseCase(request)
				.collect { result ->
					if (result is ResultState.Success) {
						_currentStep.value = ResetPasswordStep.FINISH
					}
					_resetState.value = result
				}
		}
	}


	// -- Helper Function -- //
	fun resetStateToIdle() {
		_resetState.value = ResultState.Idle
	}

	fun getSavedEmail() = savedEmail
	fun getSavedResetToken() = savedResetToken
}
