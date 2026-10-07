package com.rizkysiregar.feedzmore.data.repository.impl

import com.rizkysiregar.feedzmore.data.remote.api.UserApi
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.user.SearchUserRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.feedzmore.data.repository.UserRepository
import com.rizkysiregar.feedzmore.util.flowFromApi
import com.rizkysiregar.feedzmore.util.flowFromApiOrEmptyOn404
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
	private val retrofit: Retrofit
) : UserRepository {

	private val userApi: UserApi by lazy {
		retrofit.create(UserApi::class.java)
	}

	override fun getUserProfile(userId: String): Flow<ResultState<UserSearchItemResponse>> =
		flowFromApi {
			userApi.getUserProfile(userId)
		}

	override fun searchUsers(
		request: SearchUserRequest
	): Flow<ResultState<PaginationResponseWrapper<UserSearchItemResponse>>> =
		flowFromApiOrEmptyOn404(
			emptyValue = PaginationResponseWrapper(
				content = emptyList(),
				pageNumber = request.page,
				pageSize = request.size,
				totalElements = 0,
				totalPages = 0,
				last = true
			)
		) {
			userApi.searchUsers(
				username = request.username,
				page = request.page,
				size = request.size
			)
		}
}
