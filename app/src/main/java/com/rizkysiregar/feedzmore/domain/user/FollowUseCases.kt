package com.rizkysiregar.feedzmore.domain.user

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.feedzmore.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFollowStatusUseCase @Inject constructor(
	private val repository: UserRepository
) {
	operator fun invoke(userId: String): Flow<ResultState<FollowStatusResponse>> =
		repository.getFollowStatus(userId)
}

class GetFollowersUseCase @Inject constructor(
	private val repository: UserRepository
) {
	operator fun invoke(userId: String, page: Int, size: Int): Flow<ResultState<PaginationResponseWrapper<FollowUserResponse>>> =
		repository.getFollowers(userId, page, size)
}

class GetFollowingUseCase @Inject constructor(
	private val repository: UserRepository
) {
	operator fun invoke(userId: String, page: Int, size: Int): Flow<ResultState<PaginationResponseWrapper<FollowUserResponse>>> =
		repository.getFollowing(userId, page, size)
}

class FollowUserUseCase @Inject constructor(
	private val repository: UserRepository
) {
	operator fun invoke(userId: String): Flow<ResultState<String>> = repository.follow(userId)
}

class UnfollowUserUseCase @Inject constructor(
	private val repository: UserRepository
) {
	operator fun invoke(userId: String): Flow<ResultState<String>> = repository.unfollow(userId)
}
