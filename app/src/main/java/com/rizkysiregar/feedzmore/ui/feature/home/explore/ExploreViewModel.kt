package com.rizkysiregar.feedzmore.ui.feature.home.explore

import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetAllFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.feedzmore.domain.feed.DislikePostUseCase
import com.rizkysiregar.feedzmore.domain.feed.GetExplorePostsUseCase
import com.rizkysiregar.feedzmore.domain.feed.LikePostUseCase
import com.rizkysiregar.feedzmore.ui.feature.home.feed.BaseFeedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
	private val getExplorePostsUseCase: GetExplorePostsUseCase,
	dislikePostUseCase: DislikePostUseCase,
	likePostUseCase: LikePostUseCase,
	dataStore: DataStoreManager
) : BaseFeedViewModel(dataStore, dislikePostUseCase, likePostUseCase) {

	init {
		refresh()
	}

	override suspend fun fetchPage(
		page: Int,
		size: Int
	): Flow<ResultState<FeedsPostPaginationResponse>> =
		getExplorePostsUseCase(GetAllFeedRequest(page, size))
}
