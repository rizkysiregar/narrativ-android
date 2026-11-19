package com.rizkysiregar.narrativ.ui.feature.home.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.domain.feed.DislikePostUseCase
import com.rizkysiregar.narrativ.domain.feed.GetPostUseCase
import com.rizkysiregar.narrativ.domain.feed.LikePostUseCase
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * The post at the top of its thread. Opened from a post list it starts from the post that list handed over and
 * likes through the list, so the list's card shows the same like when the user comes back. Opened from a push or a
 * notification (or after the process was killed) it loads the post by its id and sends likes itself.
 */
@HiltViewModel
class PostThreadViewModel @Inject constructor(
	private val getPostUseCase: GetPostUseCase,
	private val likePostUseCase: LikePostUseCase,
	private val dislikePostUseCase: DislikePostUseCase
) : ViewModel() {

	private val _state = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
	val state: StateFlow<PostDetailUiState> = _state.asStateFlow()

	private var postId: String? = null
	private var loadJob: Job? = null

	// the like handler of the list the post came from; holds that list's view model, so it is dropped in onCleared
	private var listLike: ((String) -> Unit)? = null
	private var likeInFlight = false

	/** Starts the thread of [postId] once; a later call for the same post (recomposition) changes nothing. */
	fun start(postId: String, handedOver: FeedContentItem?, onListLike: ((String) -> Unit)?) {
		if (this.postId == postId) return
		this.postId = postId
		if (handedOver != null && onListLike != null) {
			listLike = onListLike
			_state.value = if (handedOver.isTakenDown) PostDetailUiState.Unavailable else PostDetailUiState.Loaded(handedOver)
		} else {
			fetch(postId)
		}
	}

	fun retry() {
		postId?.let(::fetch)
	}

	/** The post as the thread shows it now (with the user's latest like), to hand to the media viewer. */
	fun currentPost(): FeedContentItem? = (_state.value as? PostDetailUiState.Loaded)?.post

	fun toggleLike() {
		val post = currentPost() ?: return
		val wasLiked = post.liked
		setLiked(post, !wasLiked)

		val handler = listLike
		if (handler != null) {
			// the list sends the request and keeps its own card in step
			handler(post.id)
			return
		}
		if (likeInFlight) return
		val uuid = runCatching { UUID.fromString(post.id) }.getOrNull() ?: return
		likeInFlight = true
		viewModelScope.launch {
			val request = if (wasLiked) dislikePostUseCase(uuid) else likePostUseCase(uuid)
			request.collect { result ->
				when (result) {
					is ResultState.Success -> likeInFlight = false
					is ResultState.Failure -> {
						likeInFlight = false
						// put the like back the way the server still has it
						currentPost()?.let { setLiked(it, wasLiked) }
					}
					else -> Unit
				}
			}
		}
	}

	private fun setLiked(post: FeedContentItem, liked: Boolean) {
		if (post.liked == liked) return
		val count = (post.likeCount + if (liked) 1 else -1).coerceAtLeast(0)
		_state.value = PostDetailUiState.Loaded(post.copy(liked = liked, likeCount = count))
	}

	private fun fetch(postId: String) {
		val uuid = runCatching { UUID.fromString(postId) }.getOrNull()
		if (uuid == null) {
			_state.value = PostDetailUiState.Unavailable
			return
		}
		loadJob?.cancel()
		loadJob = viewModelScope.launch {
			getPostUseCase(uuid).collect { _state.value = PostDetailUiState.from(it) }
		}
	}

	override fun onCleared() {
		listLike = null
	}
}
