package com.rizkysiregar.narrativ.ui.feature.home.explore

import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.feed.GetAllFeedRequest
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.narrativ.domain.feed.DislikePostUseCase
import com.rizkysiregar.narrativ.domain.feed.GetExplorePostsUseCase
import com.rizkysiregar.narrativ.domain.feed.LikePostUseCase
import com.rizkysiregar.narrativ.ui.feature.home.feed.BaseFeedViewModel
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
		refreshAll()
	}

	override suspend fun fetchPage(
		page: Int,
		size: Int,
		cursor: String?,
		mediaOnly: Boolean
	): Flow<ResultState<FeedsPostPaginationResponse>> =
		getExplorePostsUseCase(GetAllFeedRequest(page, size, mediaOnly, cursor))
}
