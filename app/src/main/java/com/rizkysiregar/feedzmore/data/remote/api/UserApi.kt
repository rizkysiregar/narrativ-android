package com.rizkysiregar.feedzmore.data.remote.api

import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
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

	@POST("users/{userId}/follow")
	suspend fun follow(@Path("userId") userId: String): BaseResponse<String>

	@DELETE("users/{userId}/follow")
	suspend fun unfollow(@Path("userId") userId: String): BaseResponse<String>

	@GET("users/{userId}/follow-status")
	suspend fun getFollowStatus(@Path("userId") userId: String): BaseResponse<FollowStatusResponse>

	@GET("users/{userId}/followers")
	suspend fun getFollowers(
		@Path("userId") userId: String,
		@Query("page") page: Int,
		@Query("size") size: Int
	): BaseResponse<PaginationResponseWrapper<FollowUserResponse>>

	@GET("users/{userId}/following")
	suspend fun getFollowing(
		@Path("userId") userId: String,
		@Query("page") page: Int,
		@Query("size") size: Int
	): BaseResponse<PaginationResponseWrapper<FollowUserResponse>>

	@GET("users")
	suspend fun searchUsers(
		@Query("username") username: String?,
		@Query("page") page: Int,
		@Query("size") size: Int
	): BaseResponse<PaginationResponseWrapper<UserSearchItemResponse>>

}
