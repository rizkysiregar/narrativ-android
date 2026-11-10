package com.rizkysiregar.feedzmore.ui.feature.home.userdetail

import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetFeedByUsernameRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.feedzmore.domain.feed.DislikePostUseCase
import com.rizkysiregar.feedzmore.domain.feed.GetFeedByUsernameUseCase
import com.rizkysiregar.feedzmore.domain.feed.LikePostUseCase
import com.rizkysiregar.feedzmore.ui.feature.home.feed.BaseFeedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 * Description: Software Engineer
 */
@HiltViewModel
class UserDetailFeedViewModel @Inject constructor(
	private val getFeedByUsernameUseCase: GetFeedByUsernameUseCase,
	dislikePostUseCase: DislikePostUseCase,
	likePostUseCase: LikePostUseCase,
	dataStore: DataStoreManager
) : BaseFeedViewModel(dataStore, dislikePostUseCase, likePostUseCase) {

	private var targetUsername: String = ""

	fun startFor(username: String) {
		if (targetUsername == username) return
		targetUsername = username
		refreshAll()
	}

	override suspend fun fetchPage(
		page: Int,
		size: Int,
		mediaOnly: Boolean
	): Flow<ResultState<FeedsPostPaginationResponse>> =
		getFeedByUsernameUseCase(
			GetFeedByUsernameRequest(
				page = page,
				size = size,
				username = targetUsername,
				mediaOnly = mediaOnly
			)
		)
}
