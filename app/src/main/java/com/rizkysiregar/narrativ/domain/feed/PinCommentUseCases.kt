package com.rizkysiregar.narrativ.domain.feed

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/** Pins a top-level comment on top of the comments of the user's own post; one pin per post. */
class PinCommentUseCase @Inject constructor(
	private val repository: FeedRepository
) {
	operator fun invoke(postId: UUID, commentId: UUID): Flow<ResultState<String>> =
		repository.pinComment(postId, commentId)
}

class UnpinCommentUseCase @Inject constructor(
	private val repository: FeedRepository
) {
	operator fun invoke(postId: UUID): Flow<ResultState<String>> = repository.unpinComment(postId)
}
