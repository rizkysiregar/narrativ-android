package com.rizkysiregar.narrativ.data.repository

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.auth.GoogleLoginRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.LoginRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.RegisterRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.RegisterVerifyOtpRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.ResetNewPasswordRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.ResetPassVerifyOtpRequest
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ForgotPasswordResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.RegisterOtpResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.RegistrationTokenResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ResetPassVerifyOtpResponse
import kotlinx.coroutines.flow.Flow

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 */
interface AuthRepository {
	fun login(request: LoginRequest): Flow<ResultState<LoginResponse>>
	fun googleLogin(request: GoogleLoginRequest): Flow<ResultState<LoginResponse>>
	fun register(registerRequest: RegisterRequest): Flow<ResultState<String>>
	fun sendRegisterOtp(email: String): Flow<ResultState<RegisterOtpResponse>>
	fun verifyRegisterOtp(request: RegisterVerifyOtpRequest): Flow<ResultState<RegistrationTokenResponse>>
	fun forgotPassword(email: String): Flow<ResultState<ForgotPasswordResponse>>
	fun verifyOtp(request: ResetPassVerifyOtpRequest): Flow<ResultState<ResetPassVerifyOtpResponse>>
	fun resetNewPassword(request: ResetNewPasswordRequest): Flow<ResultState<String>>
}