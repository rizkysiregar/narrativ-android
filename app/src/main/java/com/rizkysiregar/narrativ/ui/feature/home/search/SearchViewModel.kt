package com.rizkysiregar.narrativ.ui.feature.home.search

import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.user.SearchUserRequest
import com.rizkysiregar.narrativ.domain.user.SearchUsersUseCase
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.telemetry.AppAnalytics
import com.rizkysiregar.narrativ.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
	dataStore: DataStoreManager,
	private val searchUsersUseCase: SearchUsersUseCase,
	private val analytics: AppAnalytics
) : BaseViewModel(dataStore) {

	private val _uiState = MutableStateFlow(SearchUserUiState())
	val uiState: StateFlow<SearchUserUiState> = _uiState.asStateFlow()

	private var searchJob: Job? = null
	private var debounceJob: Job? = null
	private var loadGeneration = 0

	fun onQueryChange(query: String) {
		_uiState.update { it.copy(query = query) }

		debounceJob?.cancel()
		debounceJob = viewModelScope.launch {
			delay(DEBOUNCE_MILLIS)
			if (_uiState.value.isQueryTooShort) reset() else search()
		}
	}

	fun retry() {
		if (_uiState.value.isQueryTooShort) reset() else search()
	}

	fun loadNextPage() {
		val state = _uiState.value
		if (state.isQueryTooShort || !state.hasLoadedInitialPage || !state.hasMore ||
			state.isLoadingMore || state.isLoading || state.isError
		) return

		_uiState.update { it.copy(isLoadingMore = true, errorMessage = null, isError = false) }

		val generation = loadGeneration
		searchJob = viewModelScope.launch {
			loadPage(page = state.page, generation = generation)
		}
	}

	private fun search() {
		analytics.log(AnalyticsEvents.SEARCH)
		loadGeneration++
		searchJob?.cancel()

		val generation = loadGeneration
		_uiState.update {
			it.copy(
				items = emptyList(),
				page = 0,
				hasLoadedInitialPage = false,
				isLoading = true,
				isLoadingMore = false,
				hasMore = false,
				errorMessage = null,
				isError = false
			)
		}

		searchJob = viewModelScope.launch {
			loadPage(page = 0, generation = generation)
		}
	}

	// too short to search: nothing found, but the text stays in the field
	private fun reset() {
		loadGeneration++
		searchJob?.cancel()
		_uiState.value = SearchUserUiState(query = _uiState.value.query)
	}

	private suspend fun loadPage(page: Int, generation: Int) {
		val state = _uiState.value

		searchUsersUseCase(
			SearchUserRequest(
				query = state.query.trim(),
				page = page,
				size = state.pageSize
			)
		).collect { result ->
			if (generation != loadGeneration) return@collect

			when (result) {
				is ResultState.Success -> {
					val response = result.data
					_uiState.update {
						it.copy(
							items = if (page == 0) response.content else it.items + response.content,
							page = page + 1,
							hasLoadedInitialPage = true,
							isLoading = false,
							isLoadingMore = false,
							hasMore = !response.last && response.content.isNotEmpty(),
							errorMessage = null,
							isError = false
						)
					}
				}

				is ResultState.Failure -> {
					_uiState.update {
						it.copy(
							isLoading = false,
							isLoadingMore = false,
							errorMessage = result.message,
							isError = true
						)
					}
				}

				else -> Unit
			}
		}
	}

	companion object {
		private const val DEBOUNCE_MILLIS = 400L
	}
}
