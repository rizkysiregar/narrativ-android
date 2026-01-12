package com.rizkysiregar.feedzmore.data.remote.api

import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.UserProfileResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 04/01/26
 * Description: Software Engineer
 */

interface UserApi {
	@GET("user/{userId}")
	suspend fun getUserProfile(
		@Path("userId") userId: String
	): BaseResponse<UserProfileResponse>

}