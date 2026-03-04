package com.rizkysiregar.feedzmore.data.repository.impl

import com.rizkysiregar.feedzmore.data.remote.api.UserApi
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.UserProfileResponse
import com.rizkysiregar.feedzmore.data.repository.UserRepository
import com.rizkysiregar.feedzmore.util.flowFromApi
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
	private val retrofit: Retrofit
) : UserRepository {

	private val userApi: UserApi by lazy {
		retrofit.create(UserApi::class.java)
	}

	override fun getUserProfile(userId: String): Flow<ResultState<UserProfileResponse>> =
		flowFromApi {
			userApi.getUserProfile(userId)
		}
}