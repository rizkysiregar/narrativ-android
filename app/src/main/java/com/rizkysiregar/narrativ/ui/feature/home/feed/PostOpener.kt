package com.rizkysiregar.narrativ.ui.feature.home.feed

import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem

/**
 * How a post list opens one of its posts. Each call hands over the list's own way to like the post ([onLike]), so
 * the list shows the same like when the user comes back.
 */
interface PostOpener {

	/** The post's thread: the post with its comments under it. [openComments] lands on the comments, ready to write. */
	fun thread(post: FeedContentItem, onLike: (String) -> Unit, openComments: Boolean = false)

	/** The post's media full screen, on [page]. */
	fun media(post: FeedContentItem, onLike: (String) -> Unit, page: Int = 0)
}
