package com.rizkysiregar.feedzmore.domain.auth

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.ResetPassVerifyOtpRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.ResetPassVerifyOtpResponse
import com.rizkysiregar.feedzmore.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 03/03/26
 * Description: Software Engineer
 */

class VerifyOtpResetPasswordUseCase @Inject constructor(
	private val authRepository: AuthRepository
) {
	operator fun invoke(request: ResetPassVerifyOtpRequest): Flow<ResultState<ResetPassVerifyOtpResponse>> {
		return authRepository.verifyOtp(request)
	}
}