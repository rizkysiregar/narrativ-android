package com.rizkysiregar.feedzmore.data.remote.api

import com.rizkysiregar.feedzmore.data.remote.model.request.auth.EmailRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.LoginRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.RegisterRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.ResetNewPasswordRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.ResetPassVerifyOtpRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.ForgotPasswordResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.ResetPassVerifyOtpResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */
interface AuthApi {

	@POST("auth/login")
	suspend fun login(
		@Body request: LoginRequest
	): BaseResponse<LoginResponse>

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