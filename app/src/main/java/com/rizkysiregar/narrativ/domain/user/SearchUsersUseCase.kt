package com.rizkysiregar.narrativ.domain.user

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.user.SearchUserRequest
import com.rizkysiregar.narrativ.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.narrativ.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.narrativ.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 */
class SearchUsersUseCase @Inject constructor(
	private val repository: UserRepository
) {
	operator fun invoke(
		request: SearchUserRequest
	): Flow<ResultState<PaginationResponseWrapper<UserSearchItemResponse>>> {
		return repository.searchUsers(request)
	}
}
