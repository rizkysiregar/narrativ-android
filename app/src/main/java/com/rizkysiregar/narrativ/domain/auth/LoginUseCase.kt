package com.rizkysiregar.narrativ.domain.auth

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.auth.LoginRequest
import com.rizkysiregar.narrativ.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.narrativ.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 */

class LoginUseCase @Inject constructor(
	private val repository: AuthRepository
) {
	operator fun invoke(request: LoginRequest): Flow<ResultState<LoginResponse>> {
		return repository.login(request)
	}
}