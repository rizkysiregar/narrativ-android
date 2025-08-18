package com.rizkysiregar.feedzmore_android_native.data.repository

import com.rizkysiregar.feedzmore_android_native.data.remote.model.LoginRequest
import com.rizkysiregar.feedzmore_android_native.data.remote.model.LoginResponse
import com.rizkysiregar.feedzmore_android_native.data.remote.model.ResultState
import kotlinx.coroutines.flow.Flow

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */
interface AuthRepository {
	fun login(request: LoginRequest): Flow<ResultState<LoginResponse>>
}