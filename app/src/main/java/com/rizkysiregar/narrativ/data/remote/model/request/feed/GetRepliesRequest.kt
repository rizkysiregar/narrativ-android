package com.rizkysiregar.narrativ.data.remote.model.request.feed

import java.util.UUID

data class GetRepliesRequest(
	// the top-level comment whose replies are loaded
	val commentId: UUID,
	val page: Int,
)
