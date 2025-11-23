package com.rizkysiregar.feedzmore.data.repository.impl

import com.rizkysiregar.feedzmore.data.remote.api.AuthApi
import com.rizkysiregar.feedzmore.data.remote.model.request.LoginRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.LoginResponse
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.repository.AuthRepository
import com.rizkysiregar.feedzmore.util.safeApiCall
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
		emit(ResultState.Loading)
		val result = safeApiCall {
			authApi.login(request)
		}

		when (result) {
			is ResultState.Success -> {
				val data = result.data

				if (data.data != null) {
					emit(ResultState.Success(data.data))
				} else {
					emit(ResultState.Failure(data.message))
				}
			}

			is ResultState.Failure -> emit(result)
			else -> {}
		}
	}
}