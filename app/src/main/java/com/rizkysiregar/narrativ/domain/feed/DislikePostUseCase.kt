package com.rizkysiregar.narrativ.domain.feed

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 05/03/26
 * Description: Software Engineer
 */

class DislikePostUseCase @Inject constructor(
	private val repository: FeedRepository
) {
	operator fun invoke(postId: UUID): Flow<ResultState<String>> {
		return repository.dislikePost(postId)
	}
}