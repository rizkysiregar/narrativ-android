package com.rizkysiregar.feedzmore.domain.auth

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.RegisterRequest
import com.rizkysiregar.feedzmore.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 27/02/26
 * Description: Software Engineer
 */
class RegisterUseCase @Inject constructor(
	private val authRepository: AuthRepository
) {
	operator fun invoke(request: RegisterRequest): Flow<ResultState<String>> {
		return authRepository.register(request)
	}
}