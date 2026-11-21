package com.rizkysiregar.narrativ.domain.auth

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ForgotPasswordResponse
import com.rizkysiregar.narrativ.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 03/03/26
 */

class SendResetOtpUseCase @Inject constructor(
	private val authRepository: AuthRepository
) {
	operator fun invoke(email: String): Flow<ResultState<ForgotPasswordResponse>> {
		return authRepository.forgotPassword(email)
	}
}