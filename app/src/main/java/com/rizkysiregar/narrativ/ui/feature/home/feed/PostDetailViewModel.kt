package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.domain.feed.DislikePostUseCase
import com.rizkysiregar.narrativ.domain.feed.GetPostUseCase
import com.rizkysiregar.narrativ.domain.feed.LikePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.UUID
import javax.inject.Inject

/** What the post detail shows when it has to load the post itself (opened from a push notification). */
sealed interface PostDetailUiState {
	data object Loading : PostDetailUiState
	data class Loaded(val post: FeedContentItem) : PostDetailUiState

	/** Deleted, taken down, or hidden by a block: nothing to show, and retrying will not help. */
	data object Unavailable : PostDetailUiState

	/** The request failed (network, server); trying again may work. */
	data class Error(val message: String) : PostDetailUiState

	companion object {
		fun from(result: ResultState<FeedContentItem>): PostDetailUiState = when (result) {
			is ResultState.Success ->
				if (result.data.isTakenDown) Unavailable else Loaded(result.data)
			is ResultState.Failure ->
				if ((result.throwable as? HttpException)?.code() == 404) Unavailable else Error(result.message)
			ResultState.Loading, ResultState.Idle -> Loading
		}
	}
}

@HiltViewModel
class PostDetailViewModel @Inject constructor(
	private val getPostUseCase: GetPostUseCase,
	private val likePostUseCase: LikePostUseCase,
	private val dislikePostUseCase: DislikePostUseCase
) : ViewModel() {

	private val _state = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
	val state: StateFlow<PostDetailUiState> = _state.asStateFlow()

	private var loadedPostId: String? = null
	private var loadJob: Job? = null

	// the like state as the screen shows it, flipped on each tap like in the feed
	private var liked = false

	/** Loads the post once per id; [retry] forces a new request. */
	fun load(postId: String) {
		if (postId == loadedPostId) return
		loadedPostId = postId
		fetch(postId)
	}

	fun retry() {
		loadedPostId?.let(::fetch)
	}

	private fun fetch(postId: String) {
		val uuid = runCatching { UUID.fromString(postId) }.getOrNull()
		if (uuid == null) {
			_state.value = PostDetailUiState.Unavailable
			return
		}
		loadJob?.cancel()
		loadJob = viewModelScope.launch {
			getPostUseCase(uuid).collect { result ->
				val next = PostDetailUiState.from(result)
				if (next is PostDetailUiState.Loaded) liked = next.post.liked
				_state.value = next
			}
		}
	}

	fun toggleLike(postId: String) {
		val uuid = runCatching { UUID.fromString(postId) }.getOrNull() ?: return
		val wasLiked = liked
		liked = !wasLiked
		viewModelScope.launch {
			val request = if (wasLiked) dislikePostUseCase(uuid) else likePostUseCase(uuid)
			request.collect { result ->
				// the screen already shows the new state; put the flag back when the server refused
				if (result is ResultState.Failure) liked = wasLiked
			}
		}
	}
}
