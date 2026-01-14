package com.rizkysiregar.feedzmore.data.repository.impl

import com.rizkysiregar.feedzmore.data.remote.api.UserApi
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.UserProfileResponse
import com.rizkysiregar.feedzmore.data.repository.UserRepository
import com.rizkysiregar.feedzmore.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
	private val retrofit: Retrofit
) : UserRepository {

	private val userApi: UserApi by lazy {
		retrofit.create(UserApi::class.java)
	}

	override fun getUserProfile(userId: String): Flow<ResultState<UserProfileResponse>> =
		flow {
			emit(ResultState.Loading)
			val result = safeApiCall {
				userApi.getUserProfile(userId)
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