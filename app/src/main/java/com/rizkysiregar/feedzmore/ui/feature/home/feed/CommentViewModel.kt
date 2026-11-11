package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.remote.ApiMessages
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.AddCommentRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetCommentRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetRepliesRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.feedzmore.domain.feed.AddCommentUseCase
import com.rizkysiregar.feedzmore.domain.feed.GetCommentUseCase
import com.rizkysiregar.feedzmore.domain.feed.GetRepliesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 08/03/26
 * Description: Software Engineer
 */

/** Replies of one top-level comment. A comment is expanded exactly when it has an entry in [CommentViewModel.replies]. */
data class RepliesState(
	val items: List<CommentItem> = emptyList(),
	val nextPage: Int = 0,
	val hasMore: Boolean = true,
	val isLoading: Boolean = false,
	val errorMessage: String? = null
)

/** One-shot things the screen reacts to. */
sealed interface CommentEvent {
	data class ShowMessage(val message: String) : CommentEvent

	/** The comment was accepted by the server: clear the draft. */
	object Sent : CommentEvent

	/** A new top-level comment was added and the list is refreshed: show it. */
	object ScrollToBottom : CommentEvent

	/** A new reply was added and the thread is refreshed: show it. */
	data class ScrollToLastReply(val rootId: String) : CommentEvent
}

@HiltViewModel
class CommentViewModel @Inject constructor(
	private val getCommentUseCase: GetCommentUseCase,
	private val getRepliesUseCase: GetRepliesUseCase,
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

	// replies of the expanded top-level comments, by top-level comment id
	private val _replies = MutableStateFlow<Map<String, RepliesState>>(emptyMap())
	val replies: StateFlow<Map<String, RepliesState>> = _replies.asStateFlow()

	// the comment the next submit answers, null for a plain comment on the post
	private val _replyTarget = MutableStateFlow<CommentItem?>(null)
	val replyTarget: StateFlow<CommentItem?> = _replyTarget.asStateFlow()

	// state for submit or post comment
	private val _isSubmitting = MutableStateFlow(false)
	val isSubmitting = _isSubmitting.asStateFlow()

	private val eventChannel = Channel<CommentEvent>(Channel.BUFFERED)
	val events: Flow<CommentEvent> = eventChannel.receiveAsFlow()

	private var currentPage = 0
	private var currentPostId: String? = null
	private var loadJob: Job? = null
	private var loadGeneration = 0
	private val replyJobs = mutableMapOf<String, Job>()

	fun loadComments(postId: String, isRefresh: Boolean = false) {
		if (isRefresh || postId != currentPostId) {
			resetFor(postId)
		}

		if (!_hasMore.value || _isLoading.value) return

		_isLoading.value = true
		_errorMessage.value = null
		val generation = loadGeneration
		val page = currentPage
		val request = GetCommentRequest(UUID.fromString(postId), page)

		loadJob = viewModelScope.launch {
			val result = getCommentUseCase(request).awaitResult()
			if (generation != loadGeneration) return@launch
			when (result) {
				is ResultState.Success -> {
					_comments.update { it + result.data.content }
					_hasMore.value = !result.data.last && result.data.content.isNotEmpty()
					currentPage = page + 1
				}

				is ResultState.Failure -> _errorMessage.value = result.message
				else -> Unit
			}
			_isLoading.value = false
		}
	}

	fun loadMore() {
		currentPostId?.let { loadComments(it) }
	}

	/** Expands the replies of a top-level comment, or collapses them when they are already shown. */
	fun toggleReplies(rootId: String) {
		if (_replies.value.containsKey(rootId)) {
			replyJobs.remove(rootId)?.cancel()
			_replies.update { it - rootId }
		} else {
			loadMoreReplies(rootId)
		}
	}

	fun loadMoreReplies(rootId: String) {
		val current = _replies.value[rootId] ?: RepliesState()
		if (current.isLoading || !current.hasMore) return

		_replies.update { it + (rootId to current.copy(isLoading = true, errorMessage = null)) }
		val page = current.nextPage
		replyJobs[rootId] = viewModelScope.launch {
			val result = getRepliesUseCase(GetRepliesRequest(UUID.fromString(rootId), page)).awaitResult()
			_replies.update { map ->
				val state = map[rootId] ?: return@update map // collapsed while loading
				map + (rootId to when (result) {
					is ResultState.Success -> state.copy(
						items = state.items + result.data.content,
						nextPage = page + 1,
						hasMore = !result.data.last && result.data.content.isNotEmpty(),
						isLoading = false,
						errorMessage = null
					)

					is ResultState.Failure -> state.copy(isLoading = false, errorMessage = result.message)
					else -> state.copy(isLoading = false)
				})
			}
			if (result is ResultState.Success) updateReplyCount(rootId, result.data.totalElements)
		}
	}

	fun startReply(target: CommentItem) {
		if (target.isTakenDown) return
		_replyTarget.value = target
	}

	fun cancelReply() {
		_replyTarget.value = null
	}

	// send comment, or reply when a reply target is set
	fun addComment(postId: String, content: String) {
		val text = content.trim()
		if (text.isEmpty() || _isSubmitting.value) return

		val target = _replyTarget.value
		_isSubmitting.value = true
		viewModelScope.launch {
			val request = AddCommentRequest(
				postId = UUID.fromString(postId),
				content = text,
				parentId = target?.let { UUID.fromString(it.id) }
			)
			when (val result = addCommentUseCase(request).awaitResult()) {
				is ResultState.Success -> {
					_replyTarget.value = null
					eventChannel.send(CommentEvent.Sent)
					_isSubmitting.value = false
					showNewComment(postId, target)
				}

				is ResultState.Failure -> {
					_isSubmitting.value = false
					eventChannel.send(CommentEvent.ShowMessage(result.message))
				}

				else -> _isSubmitting.value = false
			}
		}
	}

	private suspend fun showNewComment(postId: String, target: CommentItem?) {
		if (postId != currentPostId) return
		if (target == null) {
			// new comments go to the end of the list, so everything before it has to be loaded
			if (refreshAllComments(postId)) {
				eventChannel.send(CommentEvent.ScrollToBottom)
			} else {
				eventChannel.send(CommentEvent.ShowMessage(REFRESH_FAILED))
			}
		} else {
			val rootId = target.rootId
			if (refreshAllReplies(rootId)) {
				eventChannel.send(CommentEvent.ScrollToLastReply(rootId))
			} else {
				eventChannel.send(CommentEvent.ShowMessage(REFRESH_FAILED))
			}
		}
	}

	/** Reloads every page and swaps the list in one go, so the screen never flashes empty. */
	private suspend fun refreshAllComments(postId: String): Boolean {
		loadGeneration++
		loadJob?.cancel()
		_isLoading.value = false
		val generation = loadGeneration

		val all = mutableListOf<CommentItem>()
		var page = 0
		var reachedEnd = false
		while (page < MAX_REFRESH_PAGES && !reachedEnd) {
			val result = getCommentUseCase(GetCommentRequest(UUID.fromString(postId), page)).awaitResult()
			if (generation != loadGeneration) return false
			if (result !is ResultState.Success) return false
			all += result.data.content
			reachedEnd = result.data.last || result.data.content.isEmpty()
			page++
		}
		_comments.value = all
		currentPage = page
		_hasMore.value = !reachedEnd
		_errorMessage.value = null
		return true
	}

	private suspend fun refreshAllReplies(rootId: String): Boolean {
		replyJobs.remove(rootId)?.cancel()

		val all = mutableListOf<CommentItem>()
		var page = 0
		var reachedEnd = false
		var total = 0
		while (page < MAX_REFRESH_PAGES && !reachedEnd) {
			val result = getRepliesUseCase(GetRepliesRequest(UUID.fromString(rootId), page)).awaitResult()
			if (result !is ResultState.Success) return false
			all += result.data.content
			total = result.data.totalElements
			reachedEnd = result.data.last || result.data.content.isEmpty()
			page++
		}
		_replies.update { it + (rootId to RepliesState(items = all, nextPage = page, hasMore = !reachedEnd)) }
		updateReplyCount(rootId, total)
		return true
	}

	private fun updateReplyCount(rootId: String, count: Int) {
		_comments.update { list ->
			list.map { if (it.id == rootId && it.replyCount != count) it.copy(replyCount = count) else it }
		}
	}

	private fun resetFor(postId: String) {
		loadGeneration++
		loadJob?.cancel()
		replyJobs.values.forEach { it.cancel() }
		replyJobs.clear()
		currentPostId = postId
		currentPage = 0
		_hasMore.value = true
		_comments.value = emptyList()
		_replies.value = emptyMap()
		_replyTarget.value = null
		_isLoading.value = false
		_errorMessage.value = null
	}

	private suspend fun <T> Flow<ResultState<T>>.awaitResult(): ResultState<T> =
		firstOrNull { it is ResultState.Success || it is ResultState.Failure }
			?: ResultState.Failure(ApiMessages.GENERIC_ERROR)

	private companion object {
		// guards against a runaway loop on very long threads
		const val MAX_REFRESH_PAGES = 20
		const val REFRESH_FAILED = "Terkirim, tapi gagal memuat ulang komentar."
	}
}
