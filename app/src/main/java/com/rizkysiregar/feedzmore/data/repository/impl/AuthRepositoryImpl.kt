package com.rizkysiregar.feedzmore.data.repository.impl

import com.rizkysiregar.feedzmore.data.remote.api.AuthApi
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.EmailRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.GoogleLoginRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.LoginRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.RegisterRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.RegisterVerifyOtpRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.ResetNewPasswordRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.ResetPassVerifyOtpRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.ForgotPasswordResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.RegisterOtpResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.RegistrationTokenResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.ResetPassVerifyOtpResponse
import com.rizkysiregar.feedzmore.data.repository.AuthRepository
import com.rizkysiregar.feedzmore.util.flowFromApi
import com.rizkysiregar.feedzmore.util.flowFromMessageApi
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */
class AuthRepositoryImpl @Inject constructor(
	private val retrofit: Retrofit
) : AuthRepository {

	private val authApi: AuthApi by lazy {
		retrofit.create(AuthApi::class.java)
	}

	override fun login(request: LoginRequest): Flow<ResultState<LoginResponse>> =
		flowFromApi {
			authApi.login(request)
		}

	override fun googleLogin(request: GoogleLoginRequest): Flow<ResultState<LoginResponse>> =
		flowFromApi {
			authApi.googleLogin(request)
		}

	override fun register(registerRequest: RegisterRequest): Flow<ResultState<String>> =
		flowFromMessageApi {
			authApi.register(registerRequest)
		}

	override fun sendRegisterOtp(email: String): Flow<ResultState<RegisterOtpResponse>> =
		flowFromApi {
			authApi.sendRegisterOtp(EmailRequest(email))
		}

	override fun verifyRegisterOtp(request: RegisterVerifyOtpRequest): Flow<ResultState<RegistrationTokenResponse>> =
		flowFromApi {
			authApi.verifyRegisterOtp(request)
		}

	override fun forgotPassword(email: String): Flow<ResultState<ForgotPasswordResponse>> =
		flowFromApi {
			authApi.forgotPassword(EmailRequest(email))
		}

	override fun verifyOtp(request: ResetPassVerifyOtpRequest): Flow<ResultState<ResetPassVerifyOtpResponse>> =
		flowFromApi {
			authApi.verifyOtp(request)
		}

	override fun resetNewPassword(request: ResetNewPasswordRequest): Flow<ResultState<String>> =
		flowFromMessageApi {
			authApi.resetNewPassword(request)
		}


}