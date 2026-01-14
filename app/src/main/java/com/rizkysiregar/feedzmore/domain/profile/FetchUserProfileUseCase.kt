package com.rizkysiregar.feedzmore.domain.profile

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.UserProfileResponse
import com.rizkysiregar.feedzmore.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 04/01/26
 * Description: Software Engineer
 */
class FetchUserProfileUseCase @Inject constructor(
	private val repo: UserRepository
) {
	operator fun invoke(userId: String): Flow<ResultState<UserProfileResponse>> {
		return repo.getUserProfile(userId)
	}
}