package com.rizkysiregar.narrativ.ui.feature.home.search

import com.rizkysiregar.narrativ.data.remote.model.response.user.UserSearchItemResponse

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 */
data class SearchUserUiState(
	val query: String = "",
	val items: List<UserSearchItemResponse> = emptyList(),
	val page: Int = 0,
	val pageSize: Int = 20,
	val hasLoadedInitialPage: Boolean = false,
	val isLoading: Boolean = false,
	val isLoadingMore: Boolean = false,
	val hasMore: Boolean = false,
	val errorMessage: String? = null,
	val isError: Boolean = false
) {
	/** The backend searches 2 characters at least; shorter terms show a hint instead. */
	val isQueryTooShort: Boolean
		get() = query.trim().length < MIN_SEARCH_LENGTH
}

const val MIN_SEARCH_LENGTH = 2
