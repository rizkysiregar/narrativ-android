package com.rizkysiregar.feedzmore.domain.feed

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.GetRepliesRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.feedzmore.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRepliesUseCase @Inject constructor(
	private val repository: FeedRepository
) {
	operator fun invoke(request: GetRepliesRequest): Flow<ResultState<PaginationResponseWrapper<CommentItem>>> {
		return repository.getReplies(request)
	}
}
