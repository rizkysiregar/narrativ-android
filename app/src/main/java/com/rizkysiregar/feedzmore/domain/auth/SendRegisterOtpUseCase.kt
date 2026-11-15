package com.rizkysiregar.feedzmore.domain.auth

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.RegisterOtpResponse
import com.rizkysiregar.feedzmore.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Software Engineer
 */
class SendRegisterOtpUseCase @Inject constructor(
	private val authRepository: AuthRepository
) {
	operator fun invoke(email: String): Flow<ResultState<RegisterOtpResponse>> {
		return authRepository.sendRegisterOtp(email)
	}
}
