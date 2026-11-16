package com.rizkysiregar.narrativ.domain.feed

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.feed.AddCommentRequest
import com.rizkysiregar.narrativ.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/03/26
 * Description: Software Engineer
 */

class AddCommentUseCase @Inject constructor(
	private val repository: FeedRepository
) {
	operator fun invoke(request: AddCommentRequest): Flow<ResultState<String>> {
		return repository.addComment(request)
	}
}