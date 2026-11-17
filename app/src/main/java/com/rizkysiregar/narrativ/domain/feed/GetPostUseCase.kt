package com.rizkysiregar.narrativ.domain.feed

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/** Loads one post by id, e.g. to open it from a push notification. */
class GetPostUseCase @Inject constructor(
	private val repository: FeedRepository
) {
	operator fun invoke(postId: UUID): Flow<ResultState<FeedContentItem>> {
		return repository.getPost(postId)
	}
}
