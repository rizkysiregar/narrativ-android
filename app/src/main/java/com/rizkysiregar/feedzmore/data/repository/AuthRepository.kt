package com.rizkysiregar.feedzmore.data.repository

import com.rizkysiregar.feedzmore.data.remote.model.request.LoginRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.LoginResponse
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import kotlinx.coroutines.flow.Flow

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */
interface AuthRepository {
	fun login(request: LoginRequest): Flow<ResultState<LoginResponse>>
}