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
	// where the next page starts; null before the first page and when the server pages by number only
	val nextCursor: String? = null,
	val pageSize: Int = 10,
	val hasLoadedInitialPage: Boolean = false,
	val isRefreshing: Boolean = false,
	val isLoadingMore: Boolean = false,
	val hasMore: Boolean = false,
	val errorMessage: String? = null,
	val isError: Boolean = false
) {
	/**
	 * Nothing to show yet and nothing failed: the first page is on its way (a first install, or a refresh, which
	 * starts from an empty state). A feed that loaded and is empty is a different thing, it has its own message.
	 */
	val showsSkeleton: Boolean
		get() = items.isEmpty() && !hasLoadedInitialPage && !isError && errorMessage == null
}
