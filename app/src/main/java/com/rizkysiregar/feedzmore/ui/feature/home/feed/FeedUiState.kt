package com.rizkysiregar.feedzmore.ui.feature.home.feed

import com.rizkysiregar.feedzmore.data.remote.model.response.FeedContentItem

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 23/11/25
 * Description: Software Engineer
 */
data class FeedUiState(
	val items: List<FeedContentItem> = emptyList(),
	val page: Int = 0,
	val pageSize: Int = 0,
	val isLoading: Boolean = false,
	val hasMore: Boolean = true,
	val errorMessage: String? = null
)
