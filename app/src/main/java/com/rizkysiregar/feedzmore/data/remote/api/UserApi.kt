package com.rizkysiregar.feedzmore.data.remote.api

import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 04/01/26
 * Description: Software Engineer
 */

interface UserApi {
	@GET("users/{userId}")
	suspend fun getUserProfile(
		@Path("userId") userId: String
	): BaseResponse<UserSearchItemResponse>

	@GET("users")
	suspend fun searchUsers(
		@Query("username") username: String?,
		@Query("page") page: Int,
		@Query("size") size: Int
	): BaseResponse<PaginationResponseWrapper<UserSearchItemResponse>>

}
