package com.rizkysiregar.narrativ.domain.feed

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 11/11/26
 * Description: Deletes a comment or reply. The server allows the author and the owner of the post.
 */
class DeleteCommentUseCase @Inject constructor(
	private val repository: FeedRepository
) {
	operator fun invoke(commentId: UUID): Flow<ResultState<String>> = repository.deleteComment(commentId)
}
