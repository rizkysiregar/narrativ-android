package com.rizkysiregar.narrativ.data.repository

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.narrativ.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.narrativ.domain.moderation.ReportReason
import com.rizkysiregar.narrativ.domain.moderation.ReportTarget
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
