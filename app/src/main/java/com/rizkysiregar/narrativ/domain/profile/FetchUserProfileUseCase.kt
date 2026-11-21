package com.rizkysiregar.narrativ.domain.profile

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.narrativ.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 04/01/26
 */
class FetchUserProfileUseCase @Inject constructor(
	private val repo: UserRepository
) {
	operator fun invoke(userId: String): Flow<ResultState<UserSearchItemResponse>> {
		return repo.getUserProfile(userId)
	}
}