package com.rizkysiregar.feedzmore.data.repository

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.feedzmore.domain.moderation.ReportReason
import com.rizkysiregar.feedzmore.domain.moderation.ReportTarget
import kotlinx.coroutines.flow.Flow

/** Reporting content and blocking users, the tools Google Play expects in an app with user content. */
interface ModerationRepository {
	fun report(
		target: ReportTarget,
		targetId: String,
		reason: ReportReason,
		description: String?
	): Flow<ResultState<String>>

	fun blockUser(userId: String): Flow<ResultState<String>>

	fun unblockUser(userId: String): Flow<ResultState<String>>

	fun getBlockedUsers(page: Int, size: Int): Flow<ResultState<PaginationResponseWrapper<FollowUserResponse>>>
}
