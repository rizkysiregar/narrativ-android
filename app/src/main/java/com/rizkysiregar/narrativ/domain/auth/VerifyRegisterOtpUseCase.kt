package com.rizkysiregar.narrativ.domain.auth

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.auth.RegisterVerifyOtpRequest
import com.rizkysiregar.narrativ.data.remote.model.response.auth.RegistrationTokenResponse
import com.rizkysiregar.narrativ.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Software Engineer
 */
class VerifyRegisterOtpUseCase @Inject constructor(
	private val authRepository: AuthRepository
) {
	operator fun invoke(request: RegisterVerifyOtpRequest): Flow<ResultState<RegistrationTokenResponse>> {
		return authRepository.verifyRegisterOtp(request)
	}
}
