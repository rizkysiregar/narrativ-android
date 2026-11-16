package com.rizkysiregar.narrativ.data.repository.impl

import com.rizkysiregar.narrativ.data.remote.api.ReportApi
import com.rizkysiregar.narrativ.data.remote.api.UserApi
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.report.CreateReportRequest
import com.rizkysiregar.narrativ.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.narrativ.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.narrativ.data.repository.ModerationRepository
import com.rizkysiregar.narrativ.domain.moderation.ReportReason
import com.rizkysiregar.narrativ.domain.moderation.ReportTarget
import com.rizkysiregar.narrativ.util.flowFromApiOrEmptyOn404
import com.rizkysiregar.narrativ.util.flowFromMessageApi
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import javax.inject.Inject

class ModerationRepositoryImpl @Inject constructor(
	retrofit: Retrofit
) : ModerationRepository {

	private val reportApi: ReportApi by lazy { retrofit.create(ReportApi::class.java) }
	private val userApi: UserApi by lazy { retrofit.create(UserApi::class.java) }

	override fun report(
		target: ReportTarget,
		targetId: String,
		reason: ReportReason,
		description: String?
	): Flow<ResultState<String>> = flowFromMessageApi {
		reportApi.createReport(
			CreateReportRequest(
				targetType = target.apiName,
				targetId = targetId,
				reason = reason.apiName,
				description = description?.trim()?.takeIf { it.isNotEmpty() }
			)
		)
	}

	override fun blockUser(userId: String): Flow<ResultState<String>> =
		flowFromMessageApi { userApi.blockUser(userId) }

	override fun unblockUser(userId: String): Flow<ResultState<String>> =
		flowFromMessageApi { userApi.unblockUser(userId) }

	override fun getBlockedUsers(
		page: Int,
		size: Int
	): Flow<ResultState<PaginationResponseWrapper<FollowUserResponse>>> =
		flowFromApiOrEmptyOn404(
			emptyValue = PaginationResponseWrapper(
				content = emptyList(),
				pageNumber = page,
				pageSize = size,
				totalElements = 0,
				totalPages = 0,
				last = true
			)
		) { userApi.getBlockedUsers(page, size) }
}
