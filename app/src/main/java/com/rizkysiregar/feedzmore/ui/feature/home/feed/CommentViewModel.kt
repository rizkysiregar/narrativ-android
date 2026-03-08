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

	// state for submit or post comment
	private val _isSubmitting = MutableStateFlow(false)
	val isSubmitting = _isSubmitting.asStateFlow()


	private var currentPage = 0
	private var isLastPage = false
	private var currentPostId: String? = null

	fun loadComments(postId: String, isRefresh: Boolean = false) {
		if (isRefresh) {
			println("## comment refreshed")
			currentPage = 0
			isLastPage = false
			_comments.value = emptyList()
		}

		if (isLastPage || _isLoading.value) return

		currentPostId = postId
		_isLoading.value = true

		val request = GetCommentRequest(UUID.fromString(postId), currentPage)

		viewModelScope.launch {
			getCommentUseCase(request).collect { result ->
				when (result) {
					is ResultState.Success -> {
						val newComments = result.data.content
						_comments.value = _comments.value + newComments

						// check if its the last page
						isLastPage = newComments.isEmpty() || currentPage >= result.data.totalPages
						if (!isLastPage) currentPage++

						_isLoading.value = false
					}

					is ResultState.Failure -> {
						_isLoading.value = false
					}

					is ResultState.Loading -> {
						_isLoading.value = true
					}

					else -> {}
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