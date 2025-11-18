package com.rizkysiregar.feedzmore.data.repository.impl

import com.rizkysiregar.feedzmore.data.remote.api.AuthApi
import com.rizkysiregar.feedzmore.data.remote.model.LoginRequest
import com.rizkysiregar.feedzmore.data.remote.model.LoginResponse
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */
class AuthRepositoryImpl @Inject constructor(
	private val authApi: AuthApi
) : AuthRepository {
	override fun login(request: LoginRequest): Flow<ResultState<LoginResponse>> = flow {
		try {
			val response = authApi.login(request)
			if (response.data != null) {
				emit(ResultState.Success(response.data))
			} else {
				emit(ResultState.Failure(response.message))
			}
		} catch (e: Exception) {
			emit(ResultState.Failure("Something went wrong!"))
		}
	}

}