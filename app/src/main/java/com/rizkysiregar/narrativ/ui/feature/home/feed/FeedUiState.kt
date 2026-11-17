package com.rizkysiregar.narrativ.ui.feature.home.feed

import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 23/11/25
 * Description: Software Engineer
 */
data class FeedUiState(
	val items: List<FeedContentItem> = emptyList(),
	val page: Int = 0,
	val pageSize: Int = 10,
	val hasLoadedInitialPage: Boolean = false,
	val isRefreshing: Boolean = false,
	val isLoadingMore: Boolean = false,
	val hasMore: Boolean = false,
	val errorMessage: String? = null,
	val isError: Boolean = false
)
