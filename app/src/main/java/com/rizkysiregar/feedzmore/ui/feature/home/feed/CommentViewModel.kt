package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.AddCommentRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetCommentRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.feedzmore.domain.feed.AddCommentUseCase
import com.rizkysiregar.feedzmore.domain.feed.GetCommentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 08/03/26
 * Description: Software Engineer
 */
@HiltViewModel
class CommentViewModel @Inject constructor(
	private val getCommentUseCase: GetCommentUseCase,
	private val addCommentUseCase: AddCommentUseCase
) : ViewModel() {

	// state for fetch comments
	private val _comments = MutableStateFlow<List<CommentItem>>(emptyList())
	val comments: StateFlow<List<CommentItem>> = _comments.asStateFlow()

	private val _isLoading = MutableStateFlow(false)
	val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
	private val _hasMore = MutableStateFlow(true)
	val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()
	private val _errorMessage = MutableStateFlow<String?>(null)
	val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

	// state for submit or post comment
	private val _isSubmitting = MutableStateFlow(false)
	val isSubmitting = _isSubmitting.asStateFlow()


	private var currentPage = 0
	private var currentPostId: String? = null
	private var loadJob: Job? = null
	private var loadGeneration = 0

	fun loadComments(postId: String, isRefresh: Boolean = false) {
		if (isRefresh || postId != currentPostId) {
			loadGeneration++
			loadJob?.cancel()
			currentPostId = postId
			currentPage = 0
			_hasMore.value = true
			_comments.value = emptyList()
			_isLoading.value = false
			_errorMessage.value = null
		}

		if (!_hasMore.value || _isLoading.value) return

		_isLoading.value = true
		_errorMessage.value = null
		val generation = loadGeneration
		val page = currentPage
		val request = GetCommentRequest(UUID.fromString(postId), page)

		loadJob = viewModelScope.launch {
			try {
				getCommentUseCase(request).collect { result ->
					if (generation != loadGeneration || postId != currentPostId) return@collect
					when (result) {
						is ResultState.Success -> {
							_comments.value = _comments.value + result.data.content
							_hasMore.value = !result.data.last && result.data.content.isNotEmpty()
							currentPage = page + 1
							_errorMessage.value = null
							_isLoading.value = false
						}
						is ResultState.Failure -> {
							_errorMessage.value = result.message
							_isLoading.value = false
						}
						else -> Unit
					}
					}
			} finally {
				if (generation == loadGeneration && postId == currentPostId) {
					_isLoading.value = false
				}
			}
		}
	}

	fun loadMore() {
		currentPostId?.let { loadComments(it) }
	}

	// send commit
	fun addComment(postId: String, content: String) {
		if (content.isBlank() || _isSubmitting.value) return

		viewModelScope.launch {
			_isSubmitting.value = true
			addCommentUseCase(
				AddCommentRequest(
					postId = UUID.fromString(postId),
					content = content
				)
			).collect { result ->
				when (result) {
					is ResultState.Success -> {
						_isSubmitting.value = false
						loadComments(postId, isRefresh = true)
					}

					is ResultState.Failure -> {
						_isSubmitting.value = false
					}

					is ResultState.Loading -> {
						_isSubmitting.value = true
					}

					else -> {
						_isSubmitting.value = false
					}
				}
			}
		}
	}


}
