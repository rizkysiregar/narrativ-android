package com.rizkysiregar.feedzmore.domain.user

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.user.SearchUserRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.feedzmore.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 * Description: Software Engineer
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
