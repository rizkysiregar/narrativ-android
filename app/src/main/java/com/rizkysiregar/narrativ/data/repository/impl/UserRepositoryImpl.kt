package com.rizkysiregar.narrativ.data.repository.impl

import com.rizkysiregar.narrativ.data.remote.model.request.user.ChangeEmailOtpRequest
import com.rizkysiregar.narrativ.data.remote.model.request.user.ConfirmEmailChangeRequest
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ForgotPasswordResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.narrativ.data.remote.api.UserApi
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.user.DeleteAccountRequest
import com.rizkysiregar.narrativ.data.remote.model.request.user.SearchUserRequest
import com.rizkysiregar.narrativ.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.narrativ.data.remote.model.response.user.FollowStatusResponse
import com.rizkysiregar.narrativ.data.remote.model.request.user.CreateProfileImageUploadUrlRequest
import com.rizkysiregar.narrativ.data.remote.model.request.user.SetProfileImageRequest
import com.rizkysiregar.narrativ.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.narrativ.data.remote.model.response.user.ProfileImageUploadUrlResponse
import com.rizkysiregar.narrativ.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.narrativ.data.repository.UserRepository
import com.rizkysiregar.narrativ.util.flowFromApi
import com.rizkysiregar.narrativ.util.flowFromMessageApi
import com.rizkysiregar.narrativ.util.flowFromApiOrEmptyOn404
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
	private val retrofit: Retrofit
) : UserRepository {

	private val userApi: UserApi by lazy {
		retrofit.create(UserApi::class.java)
	}

	override suspend fun createProfileImageUploadUrl(contentType: String): ProfileImageUploadUrlResponse {
		val response = userApi.createProfileImageUploadUrl(CreateProfileImageUploadUrlRequest(contentType))
		return response.data ?: throw IllegalStateException(response.message)
	}

	override suspend fun setProfileImage(objectKey: String): UserSearchItemResponse {
		val response = userApi.setProfileImage(SetProfileImageRequest(objectKey))
		return response.data ?: throw IllegalStateException(response.message)
	}

	override suspend fun deleteProfileImage(): UserSearchItemResponse {
		val response = userApi.deleteProfileImage()
		return response.data ?: throw IllegalStateException(response.message)
	}

	override fun sendEmailChangeOtp(email: String): Flow<ResultState<ForgotPasswordResponse>> =
		flowFromApi { userApi.sendEmailChangeOtp(ChangeEmailOtpRequest(email)) }

	override fun confirmEmailChange(request: ConfirmEmailChangeRequest): Flow<ResultState<LoginResponse>> =
		flowFromApi { userApi.confirmEmailChange(request) }

	override fun deleteAccount(request: DeleteAccountRequest): Flow<ResultState<String>> =
		flowFromMessageApi { userApi.deleteAccount(request) }

	override fun getUserProfile(userId: String): Flow<ResultState<UserSearchItemResponse>> =
		flowFromApi {
			userApi.getUserProfile(userId)
		}

	override fun follow(userId: String): Flow<ResultState<String>> =
		flowFromMessageApi { userApi.follow(userId) }

	override fun unfollow(userId: String): Flow<ResultState<String>> =
		flowFromMessageApi { userApi.unfollow(userId) }

	override fun getFollowStatus(userId: String): Flow<ResultState<FollowStatusResponse>> =
		flowFromApi { userApi.getFollowStatus(userId) }

	override fun getFollowers(
		userId: String,
		page: Int,
		size: Int
	): Flow<ResultState<PaginationResponseWrapper<FollowUserResponse>>> =
		flowFromApi { userApi.getFollowers(userId, page, size) }

	override fun getFollowing(
		userId: String,
		page: Int,
		size: Int
	): Flow<ResultState<PaginationResponseWrapper<FollowUserResponse>>> =
		flowFromApi { userApi.getFollowing(userId, page, size) }

	override fun searchUsers(
		request: SearchUserRequest
	): Flow<ResultState<PaginationResponseWrapper<UserSearchItemResponse>>> =
		flowFromApiOrEmptyOn404(
			emptyValue = PaginationResponseWrapper(
				content = emptyList(),
				pageNumber = request.page,
				pageSize = request.size,
				totalElements = 0,
				totalPages = 0,
				last = true
			)
		) {
			userApi.searchUsers(
				username = request.username,
				page = request.page,
				size = request.size
			)
		}
}
