package com.rizkysiregar.feedzmore.data.remote.api

import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse
import com.rizkysiregar.feedzmore.data.remote.model.request.LoginRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */
interface AuthApi {

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): BaseResponse<LoginResponse>
}