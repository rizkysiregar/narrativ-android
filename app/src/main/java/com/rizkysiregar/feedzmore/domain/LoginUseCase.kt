package com.rizkysiregar.feedzmore.domain

import com.rizkysiregar.feedzmore.data.remote.model.LoginRequest
import com.rizkysiregar.feedzmore.data.remote.model.LoginResponse
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */

class LoginUseCase @Inject constructor(
	private val repository: AuthRepository
) {
	operator fun invoke(request: LoginRequest): Flow<ResultState<LoginResponse>> {
		return repository.login(request)
	}
}