package com.rizkysiregar.feedzmore.data.repository

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.UserProfileResponse
import kotlinx.coroutines.flow.Flow

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 04/01/26
 * Description: Software Engineer
 */
interface UserRepository {
	fun getUserProfile(userId: String): Flow<ResultState<UserProfileResponse>>
}