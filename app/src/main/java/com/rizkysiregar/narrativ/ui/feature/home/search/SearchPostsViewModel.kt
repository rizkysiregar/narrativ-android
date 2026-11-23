package com.rizkysiregar.narrativ.ui.feature.home.search

import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.feed.SearchPostsRequest
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.narrativ.domain.feed.DislikePostUseCase
import com.rizkysiregar.narrativ.domain.feed.LikePostUseCase
import com.rizkysiregar.narrativ.domain.feed.SearchPostsUseCase
import com.rizkysiregar.narrativ.ui.feature.home.feed.BaseFeedViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The posts a search on home finds, paged like a feed (a like shows at once and goes back if the server refuses).
 * Searches once the typing pauses, like [SearchViewModel]; a term shorter than [MIN_SEARCH_LENGTH] searches nothing.
 */
@HiltViewModel
class SearchPostsViewModel @Inject constructor(
	private val searchPostsUseCase: SearchPostsUseCase,
	dislikePostUseCase: DislikePostUseCase,
	likePostUseCase: LikePostUseCase,
	dataStore: DataStoreManager
) : BaseFeedViewModel(dataStore, dislikePostUseCase, likePostUseCase) {

	// the term the shown results are for
	private var query = ""
	private var debounceJob: Job? = null

	fun onQueryChange(raw: String) {
		val next = raw.trim()
		debounceJob?.cancel()
		debounceJob = viewModelScope.launch {
			delay(DEBOUNCE_MILLIS)
			if (next.length >= MIN_SEARCH_LENGTH && next != query) {
				query = next
				refresh()
			} else if (next.length < MIN_SEARCH_LENGTH) {
				query = ""
			}
		}
	}

	override suspend fun fetchPage(
		page: Int,
		size: Int,
		cursor: String?,
		mediaOnly: Boolean
	): Flow<ResultState<FeedsPostPaginationResponse>> =
		if (query.isEmpty()) emptyFlow() else searchPostsUseCase(SearchPostsRequest(query, size, cursor))

	companion object {
		private const val DEBOUNCE_MILLIS = 400L
	}
}
