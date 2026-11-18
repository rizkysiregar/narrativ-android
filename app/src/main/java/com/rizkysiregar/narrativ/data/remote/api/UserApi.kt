package com.rizkysiregar.narrativ.data.remote.api

import com.rizkysiregar.narrativ.data.remote.model.request.user.ChangeEmailOtpRequest
import com.rizkysiregar.narrativ.data.remote.model.request.user.ConfirmEmailChangeRequest
import com.rizkysiregar.narrativ.data.remote.model.request.user.CreateProfileImageUploadUrlRequest
import com.rizkysiregar.narrativ.data.remote.model.request.user.DeleteAccountRequest
import com.rizkysiregar.narrativ.data.remote.model.request.user.SetProfileImageRequest
import com.rizkysiregar.narrativ.data.remote.model.response.BaseResponse
import com.rizkysiregar.narrativ.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ForgotPasswordResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.narrativ.data.remote.model.response.user.FollowStatusResponse
import com.rizkysiregar.narrativ.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.narrativ.data.remote.model.response.user.ProfileImageUploadUrlResponse
import com.rizkysiregar.narrativ.data.remote.model.response.user.UserSearchItemResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 04/01/26
 * Description: Software Engineer
 */

interface UserApi {
	@POST("users/me/profile-image/upload-url")
	suspend fun createProfileImageUploadUrl(
		@Body request: CreateProfileImageUploadUrlRequest
	): BaseResponse<ProfileImageUploadUrlResponse>

	@PUT("users/me/profile-image")
	suspend fun setProfileImage(
		@Body request: SetProfileImageRequest
	): BaseResponse<UserSearchItemResponse>

	@DELETE("users/me/profile-image")
	suspend fun deleteProfileImage(): BaseResponse<UserSearchItemResponse>

	/** Sends an OTP to the new email of the account. */
	@POST("users/me/email/otp")
	suspend fun sendEmailChangeOtp(@Body request: ChangeEmailOtpRequest): BaseResponse<ForgotPasswordResponse>

	/** Changes the email; every other session ends and the answer is a new token pair for this device. */
	@POST("users/me/email")
	suspend fun confirmEmailChange(@Body request: ConfirmEmailChangeRequest): BaseResponse<LoginResponse>

	/** Deletes the account and its personal data for good. */
	@POST("users/me/deletion")
	suspend fun deleteAccount(@Body request: DeleteAccountRequest): BaseResponse<String>

	@POST("users/{userId}/block")
	suspend fun blockUser(@Path("userId") userId: String): BaseResponse<String>

	@DELETE("users/{userId}/block")
	suspend fun unblockUser(@Path("userId") userId: String): BaseResponse<String>

	@GET("users/me/blocks")
	suspend fun getBlockedUsers(
		@Query("page") page: Int,
		@Query("size") size: Int
	): BaseResponse<PaginationResponseWrapper<FollowUserResponse>>

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
