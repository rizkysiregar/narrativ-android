package com.rizkysiregar.feedzmore.ui.feature.home.search

import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 * Description: Software Engineer
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
)
