package com.rizkysiregar.narrativ.data.remote.api

import com.rizkysiregar.narrativ.data.remote.model.request.auth.EmailRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.GoogleLoginRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.LoginRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.RefreshTokenRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.RegisterRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.RegisterVerifyOtpRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.ResetNewPasswordRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.ResetPassVerifyOtpRequest
import com.rizkysiregar.narrativ.data.remote.model.response.BaseResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ForgotPasswordResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.RegisterOtpResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.RegistrationTokenResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ResetPassVerifyOtpResponse
import okhttp3.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 */
interface AuthApi {

	@POST("auth/login")
	suspend fun login(
		@Body request: LoginRequest
	): BaseResponse<LoginResponse>

	@POST("auth/refresh")
	suspend fun refreshToken(
		@Body request: RefreshTokenRequest
	): BaseResponse<LoginResponse>

	/** Revokes the refresh token on the server. Public endpoint: no access token is sent. */
	@POST("auth/logout")
	suspend fun logout(
		@Body request: RefreshTokenRequest
	): BaseResponse<String>

	@POST("auth/google")
	suspend fun googleLogin(
		@Body request: GoogleLoginRequest
	): BaseResponse<LoginResponse>

	@POST("auth/register/otp")
	suspend fun sendRegisterOtp(
		@Body emailRequest: EmailRequest
	): BaseResponse<RegisterOtpResponse>

	@POST("auth/register/verify-otp")
	suspend fun verifyRegisterOtp(
		@Body request: RegisterVerifyOtpRequest
	): BaseResponse<RegistrationTokenResponse>

	@POST("auth/register")
	suspend fun register(
		@Body request: RegisterRequest
	): BaseResponse<String>

	@POST("auth/forgot-password")
	suspend fun forgotPassword(
		@Body emailRequest: EmailRequest
	): BaseResponse<ForgotPasswordResponse>

	@POST("auth/verify-otp")
	suspend fun verifyOtp(
		@Body request: ResetPassVerifyOtpRequest
	): BaseResponse<ResetPassVerifyOtpResponse>

	@POST("auth/reset-password")
	suspend fun resetNewPassword(
		@Body request: ResetNewPasswordRequest
	): BaseResponse<String>

}