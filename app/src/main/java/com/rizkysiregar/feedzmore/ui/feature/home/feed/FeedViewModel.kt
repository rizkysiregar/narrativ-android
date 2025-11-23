package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.GetFeedPostRequest
import com.rizkysiregar.feedzmore.domain.feed.GetFeedPostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@HiltViewModel
class FeedViewModel @Inject constructor(
	private val getFeedPostUseCase: GetFeedPostUseCase
) : ViewModel() {

	private val _uiState = MutableStateFlow(FeedUiState())
	val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

	// prevent concurrent loads
	private val loadingMutex = Mutex()


	init {
		loadNextPage()
	}

	fun loadNextPage() {
		// do not start if already loading (quick guard)
		if (_uiState.value.isLoading) return

		viewModelScope.launch {
			// ensure only one loadNextPage body runs at a time
			if (!loadingMutex.tryLock()) return@launch

			try {
				_uiState.update { it.copy(isLoading = true, errorMessage = null) }
				val pageToRequest = _uiState.value.page
				val pageSize = _uiState.value.pageSize

				getFeedPostUseCase(
					GetFeedPostRequest(
						page = pageToRequest,
						size = pageSize,
						userId = "123"
					)
				).collect { result ->
					when (result) {
						is ResultState.Loading -> {}

						is ResultState.Success -> {
							val response = result.data

							// if last page -> restart from page 0 (endless loop behavior)
							if (response.last) {

								// fetch page 0 and append (attempt restart)
								// call use case synchronously here
								getFeedPostUseCase(
									GetFeedPostRequest(
										page = 0,
										size = pageSize,
										"123"
									)
								)
									.firstOrNull()?.let { restartRes ->

										when (restartRes) {
											is ResultState.Success -> {
												_uiState.update {
													it.copy(
														items = it.items + restartRes.data.content,
														page = 1, // after 0
														isLoading = false,
														hasMore = true
													)
												}
											}

											is ResultState.Failure -> {
												_uiState.update {
													it.copy(
														isLoading = false,
														errorMessage = restartRes.message
													)
												}
											}

											else -> {
												_uiState.update { it.copy(isLoading = false) }
											}
										}

									} ?: run {
									_uiState.update { it.copy(isLoading = true) }
								}
							} else {
								// normal append
								_uiState.update {
									it.copy(
										items = it.items + response.content,
										page = pageToRequest + 1,
										isLoading = false,
										hasMore = true
									)
								}
							}
						}

						is ResultState.Failure -> {
							_uiState.update {
								it.copy(isLoading = false, errorMessage = result.message)
							}
						}

						is ResultState.Idle -> {}
					}
				}


			} finally {
				loadingMutex.unlock()
			}

		}

		fun refresh() {
			viewModelScope.launch {
				// clear and lead fresh from page 0
				_uiState.update {
					it.copy(
						items = emptyList(),
						page = 0,
						hasMore = true,
						errorMessage = null
					)
				}

				loadNextPage()
			}
		}


	}
}