package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.data.remote.ApiMessages
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.feed.AddCommentRequest
import com.rizkysiregar.narrativ.data.remote.model.request.feed.GetCommentRequest
import com.rizkysiregar.narrativ.data.remote.model.request.feed.GetRepliesRequest
import com.rizkysiregar.narrativ.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.domain.feed.AddCommentUseCase
import com.rizkysiregar.narrativ.domain.feed.DeleteCommentUseCase
import com.rizkysiregar.narrativ.domain.feed.GetCommentUseCase
import com.rizkysiregar.narrativ.domain.feed.GetRepliesUseCase
import com.rizkysiregar.narrativ.domain.feed.PinCommentUseCase
import com.rizkysiregar.narrativ.domain.feed.UnpinCommentUseCase
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.telemetry.AppAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import com.rizkysiregar.narrativ.i18n.AppStrings
import com.rizkysiregar.narrativ.R

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

	/** The comment is on its way (shown as pending): clear the draft and close the keyboard. */
	object Sent : CommentEvent

	/** Sending failed: put the text back so nothing the user typed is lost. */
	data class SendFailed(val text: String) : CommentEvent

	/** A new top-level comment was added and the list is refreshed: show it. */
	object ScrollToBottom : CommentEvent

	/** A new reply was added and the thread is refreshed: show it. */
	data class ScrollToLastReply(val rootId: String) : CommentEvent
}

@HiltViewModel
class CommentViewModel @Inject constructor(
	private val getCommentUseCase: GetCommentUseCase,
	private val getRepliesUseCase: GetRepliesUseCase,
	private val addCommentUseCase: AddCommentUseCase,
	private val deleteCommentUseCase: DeleteCommentUseCase,
	private val pinCommentUseCase: PinCommentUseCase,
	private val unpinCommentUseCase: UnpinCommentUseCase,
	private val dataStore: DataStoreManager,
	private val analytics: AppAnalytics,
	private val appStrings: AppStrings
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

	// the signed-in user, so the screen knows when it shows their own post (they may pin its comments)
	val currentUserId: StateFlow<String> = dataStore.get(PrefKeys.userId, "")
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

	// words of the caption the next top-level comment quotes; a quote and a reply exclude each other
	private val _quote = MutableStateFlow<String?>(null)
	val quote: StateFlow<String?> = _quote.asStateFlow()

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
	private val deleting = mutableSetOf<String>()
	private var pinJob: Job? = null

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

	/**
	 * Loads the first page of [postId]'s comments unless they are already there, so a screen that comes back (from
	 * the media viewer, say) keeps its comments, open replies and scroll position.
	 */
	fun start(postId: String) {
		if (postId != currentPostId) loadComments(postId, isRefresh = true)
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
		_quote.value = null
		_replyTarget.value = target
	}

	/** Quotes [text] of the caption in the next comment, which then answers the post rather than a comment. */
	fun startQuote(text: String) {
		val quote = text.trim().take(MAX_QUOTE_LENGTH)
		if (quote.isEmpty()) return
		_replyTarget.value = null
		_quote.value = quote
	}

	fun cancelQuote() {
		_quote.value = null
	}

	/**
	 * Pins [comment] on top of the comments, or unpins it when it is the pinned one. Only the post's author may; the
	 * list changes at once and goes back when the server refuses.
	 */
	fun togglePin(postId: String, comment: CommentItem) {
		if (comment.isReply || comment.isPending || pinJob?.isActive == true) return
		val before = _comments.value
		val pin = !comment.pinned
		_comments.value = withPinned(before, if (pin) comment.id else null)
		pinJob = viewModelScope.launch {
			val uuid = UUID.fromString(postId)
			val request = if (pin) pinCommentUseCase(uuid, UUID.fromString(comment.id)) else unpinCommentUseCase(uuid)
			when (val result = request.awaitResult()) {
				is ResultState.Success -> eventChannel.send(
					CommentEvent.ShowMessage(appStrings.get(if (pin) R.string.comment_pinned else R.string.comment_unpinned))
				)

				is ResultState.Failure -> {
					_comments.value = before
					eventChannel.send(CommentEvent.ShowMessage(result.message))
				}

				else -> Unit
			}
		}
	}

	fun cancelReply() {
		_replyTarget.value = null
	}

	// send comment, or reply when a reply target is set; it shows up at once as a pending row
	fun addComment(postId: String, content: String) {
		val text = content.trim()
		if (text.isEmpty() || _isSubmitting.value) return

		val target = _replyTarget.value
		// a quote answers the post, so it never goes with a reply
		val quote = _quote.value.takeIf { target == null }
		_isSubmitting.value = true
		viewModelScope.launch {
			val pending = CommentItem(
				id = "pending-${UUID.randomUUID()}",
				username = dataStore.get(PrefKeys.username, "").first(),
				// so a comment by the post's author shows as theirs while it is sending
				userId = dataStore.get(PrefKeys.userId, "").first().takeIf { it.isNotBlank() },
				commentContent = text,
				createdAt = Instant.now().toString(),
				parentId = target?.rootId,
				replyToCommentId = target?.id,
				replyToUsername = target?.username,
				quoteText = quote,
				isPending = true
			)
			val wasExpanded = target != null && _replies.value.containsKey(target.rootId)
			insertPending(pending, target)
			_replyTarget.value = null
			_quote.value = null
			eventChannel.send(CommentEvent.Sent)
			eventChannel.send(if (target == null) CommentEvent.ScrollToBottom else CommentEvent.ScrollToLastReply(target.rootId))

			val request = AddCommentRequest(
				postId = UUID.fromString(postId),
				content = text,
				parentId = target?.let { UUID.fromString(it.id) },
				quoteText = quote
			)
			when (val result = addCommentUseCase(request).awaitResult()) {
				is ResultState.Success -> {
					analytics.log(AnalyticsEvents.COMMENT_ADDED, AnalyticsEvents.Param.IS_REPLY to (target != null))
					replacePending(pending, pending.copy(isPending = false))
					_isSubmitting.value = false
					showNewComment(postId, target)
				}

				else -> {
					analytics.log(AnalyticsEvents.COMMENT_FAILED, AnalyticsEvents.Param.IS_REPLY to (target != null))
					removePending(pending, wasExpanded)
					_replyTarget.value = target
					_quote.value = quote
					_isSubmitting.value = false
					eventChannel.send(CommentEvent.SendFailed(text))
					eventChannel.send(
						CommentEvent.ShowMessage((result as? ResultState.Failure)?.message ?: ApiMessages.GENERIC_ERROR)
					)
				}
			}
		}
	}

	private fun insertPending(pending: CommentItem, target: CommentItem?) {
		if (target == null) {
			_comments.update { it + pending }
		} else {
			_replies.update { map ->
				val state = map[target.rootId] ?: RepliesState(hasMore = false)
				map + (target.rootId to state.copy(items = state.items + pending))
			}
		}
	}

	private fun replacePending(pending: CommentItem, with: CommentItem) {
		_comments.update { list -> list.map { if (it.id == pending.id) with else it } }
		_replies.update { map ->
			map.mapValues { (_, state) -> state.copy(items = state.items.map { if (it.id == pending.id) with else it }) }
		}
	}

	private fun removePending(pending: CommentItem, replyThreadWasExpanded: Boolean) {
		_comments.update { list -> list.filterNot { it.id == pending.id } }
		_replies.update { map ->
			val rootId = pending.parentId ?: return@update map
			val state = map[rootId] ?: return@update map
			val items = state.items.filterNot { it.id == pending.id }
			// a thread opened only to show the pending reply goes back to collapsed
			if (items.isEmpty() && !replyThreadWasExpanded) map - rootId else map + (rootId to state.copy(items = items))
		}
	}

	/** Deletes [comment] on the server, then drops it (and the replies of a top-level comment) from the lists. */
	fun deleteComment(comment: CommentItem) {
		if (comment.isPending || !deleting.add(comment.id)) return
		viewModelScope.launch {
			try {
				when (val result = deleteCommentUseCase(UUID.fromString(comment.id)).awaitResult()) {
					is ResultState.Success -> {
						removeLocally(comment)
						eventChannel.send(CommentEvent.ShowMessage(appStrings.get(if (comment.isReply) R.string.reply_deleted else R.string.comment_deleted)))
					}

					is ResultState.Failure -> eventChannel.send(CommentEvent.ShowMessage(result.message))
					else -> Unit
				}
			} finally {
				deleting.remove(comment.id)
			}
		}
	}

	private fun removeLocally(comment: CommentItem) {
		val target = _replyTarget.value
		if (target != null && (target.id == comment.id || (!comment.isReply && target.rootId == comment.id))) {
			_replyTarget.value = null
		}

		if (!comment.isReply) {
			replyJobs.remove(comment.id)?.cancel()
			_replies.update { it - comment.id }
			_comments.update { list -> list.filterNot { it.id == comment.id } }
			return
		}

		val rootId = comment.rootId
		_replies.update { map ->
			val state = map[rootId] ?: return@update map
			val items = state.items.filterNot { it.id == comment.id }.map {
				// the server clears the tag of answers to a deleted reply, do the same here
				if (it.replyToCommentId == comment.id) it.copy(replyToCommentId = null, replyToUsername = null) else it
			}
			if (items.isEmpty() && !state.hasMore) map - rootId else map + (rootId to state.copy(items = items))
		}
		_comments.update { list ->
			list.map { if (it.id == rootId) it.copy(replyCount = (it.replyCount - 1).coerceAtLeast(0)) else it }
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

// the server stores a quote in VARCHAR(300)
const val MAX_QUOTE_LENGTH = 300

/**
 * The comments as the server lists them with [pinnedId] pinned: the pinned one first, the rest oldest first. Without
 * a pin every comment goes back to its place.
 */
internal fun withPinned(comments: List<CommentItem>, pinnedId: String?): List<CommentItem> {
	val updated = comments.map { it.copy(pinned = it.id == pinnedId) }
	val (pinned, rest) = updated.partition { it.pinned }
	return pinned + rest.sortedWith(compareBy(nullsLast<Instant>()) { it.createdAt?.let(::parseInstantOrNull) })
}

private fun parseInstantOrNull(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()
