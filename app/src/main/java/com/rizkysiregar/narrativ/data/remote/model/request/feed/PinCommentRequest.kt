package com.rizkysiregar.narrativ.data.remote.model.request.feed

import java.util.UUID

/** The top-level comment the post's author pins on top of its comments. */
data class PinCommentRequest(val commentId: UUID)
