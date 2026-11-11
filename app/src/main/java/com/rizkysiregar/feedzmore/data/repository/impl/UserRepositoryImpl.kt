package com.rizkysiregar.feedzmore.data.repository.impl

import com.rizkysiregar.feedzmore.data.remote.api.UserApi
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.user.SearchUserRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse
import com.rizkysiregar.feedzmore.data.remote.model.request.user.CreateProfileImageUploadUrlRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.user.SetProfileImageRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.user.ProfileImageUploadUrlResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.feedzmore.data.repository.UserRepository
import com.rizkysiregar.feedzmore.util.flowFromApi
import com.rizkysiregar.feedzmore.util.flowFromMessageApi
import com.rizkysiregar.feedzmore.util.flowFromApiOrEmptyOn404
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
