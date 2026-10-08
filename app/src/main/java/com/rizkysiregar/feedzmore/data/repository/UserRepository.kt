package com.rizkysiregar.feedzmore.data.repository

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.user.SearchUserRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import kotlinx.coroutines.flow.Flow

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 04/01/26
 * Description: Software Engineer
 */
interface UserRepository {
	fun getUserProfile(userId: String): Flow<ResultState<UserSearchItemResponse>>

	fun searchUsers(request: SearchUserRequest): Flow<ResultState<PaginationResponseWrapper<UserSearchItemResponse>>>
	fun follow(userId: String): Flow<ResultState<String>>
	fun unfollow(userId: String): Flow<ResultState<String>>
	fun getFollowStatus(userId: String): Flow<ResultState<FollowStatusResponse>>
	fun getFollowers(userId: String, page: Int, size: Int): Flow<ResultState<PaginationResponseWrapper<FollowUserResponse>>>
	fun getFollowing(userId: String, page: Int, size: Int): Flow<ResultState<PaginationResponseWrapper<FollowUserResponse>>>
}
