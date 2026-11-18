package com.rizkysiregar.narrativ.testing

import com.rizkysiregar.narrativ.data.remote.model.response.PaginationResponseWrapper
import com.rizkysiregar.narrativ.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.narrativ.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedsPostPaginationResponse
import java.util.UUID

/** Builders with sensible defaults, so a test only spells out what it is about. */

fun uuid(): String = UUID.randomUUID().toString()

fun aFeedItem(
	id: String = uuid(),
	username: String = "rizky",
	caption: String? = "hello",
	liked: Boolean = false,
	likeCount: Int = 0,
	commentCount: Int = 0,
	moderationStatus: String? = null
) = FeedContentItem(
	createdAt = "2026-01-01T00:00:00Z",
	caption = caption,
	likeCount = likeCount,
	id = id,
	liked = liked,
	username = username,
	commentCount = commentCount,
	moderationStatus = moderationStatus
)

fun aFeedPage(
	content: List<FeedContentItem> = emptyList(),
	last: Boolean = true,
	pageNumber: Int = 0,
	nextCursor: String? = null
) = FeedsPostPaginationResponse(
	pageNumber = pageNumber,
	last = last,
	totalPages = if (last) pageNumber + 1 else pageNumber + 2,
	pageSize = content.size,
	content = content,
	totalElements = content.size,
	nextCursor = nextCursor
)

fun aComment(
	id: String = uuid(),
	username: String = "rizky",
	content: String? = "nice",
	parentId: String? = null,
	replyCount: Int = 0
) = CommentItem(
	id = id,
	username = username,
	commentContent = content,
	parentId = parentId,
	replyCount = replyCount
)

fun <T> aPage(
	content: List<T>,
	last: Boolean = true,
	pageNumber: Int = 0,
	totalElements: Int = content.size
) = PaginationResponseWrapper(
	pageNumber = pageNumber,
	last = last,
	totalPages = if (last) pageNumber + 1 else pageNumber + 2,
	pageSize = content.size,
	content = content,
	totalElements = totalElements
)

fun aLoginResponse(
	token: String = "access-1",
	refreshToken: String = "refresh-1",
	username: String = "rizky",
	userId: String = uuid()
) = LoginResponse(token = token, refreshToken = refreshToken, username = username, userId = userId)
