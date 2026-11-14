package com.rizkysiregar.feedzmore.data.repository

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.user.DeleteAccountRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.user.SearchUserRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.user.ProfileImageUploadUrlResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import kotlinx.coroutines.flow.Flow

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 04/01/26
 * Description: Software Engineer
 */
interface UserRepository {
	/** Each of these throws when the call fails, like [PostRepository.createUploadUrl]. */
	suspend fun createProfileImageUploadUrl(contentType: String): ProfileImageUploadUrlResponse
	suspend fun setProfileImage(objectKey: String): UserSearchItemResponse
	suspend fun deleteProfileImage(): UserSearchItemResponse

	/** The account is gone on the server when this succeeds; the caller clears the local session. */
	fun deleteAccount(request: DeleteAccountRequest): Flow<ResultState<String>>

	fun getUserProfile(userId: String): Flow<ResultState<UserSearchItemResponse>>

	fun searchUsers(request: SearchUserRequest): Flow<ResultState<PaginationResponseWrapper<UserSearchItemResponse>>>
	fun follow(userId: String): Flow<ResultState<String>>
	fun unfollow(userId: String): Flow<ResultState<String>>
	fun getFollowStatus(userId: String): Flow<ResultState<FollowStatusResponse>>
	fun getFollowers(userId: String, page: Int, size: Int): Flow<ResultState<PaginationResponseWrapper<FollowUserResponse>>>
	fun getFollowing(userId: String, page: Int, size: Int): Flow<ResultState<PaginationResponseWrapper<FollowUserResponse>>>
}
