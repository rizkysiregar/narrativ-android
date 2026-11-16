package com.rizkysiregar.narrativ.domain.feed

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.feed.GetCommentRequest
import com.rizkysiregar.narrativ.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.narrativ.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.narrativ.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/03/26
 * Description: Software Engineer
 */
class GetCommentUseCase @Inject constructor(
	private val repository: FeedRepository
) {
	operator fun invoke(request: GetCommentRequest): Flow<ResultState<PaginationResponseWrapper<CommentItem>>> {
		return repository.getComment(request)
	}
}