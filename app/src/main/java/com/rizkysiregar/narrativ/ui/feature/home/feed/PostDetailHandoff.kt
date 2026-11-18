package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.lifecycle.ViewModel
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem

/**
 * What a post list hands to the detail screen it opens: the post, so it shows at once, and the list's own way to
 * like it, so the list shows the same like when the user comes back.
 *
 * This lives in a ViewModel and not in the SavedStateHandle of the back stack entry on purpose. That handle is written
 * into a Bundle every time the Activity stops, and a function reference holding a ViewModel cannot be written there:
 * the app crashed with a NotSerializableException as soon as the user pressed Home on a post. A ViewModel is not
 * saved, so after the process was killed nothing is handed over and the detail loads the post by its id, which is
 * what it does for a push notification anyway.
 */
class PostDetailHandoff : ViewModel() {

	private var post: FeedContentItem? = null
	private var onLike: ((String) -> Unit)? = null

	fun hand(post: FeedContentItem, onLike: (String) -> Unit) {
		this.post = post
		this.onLike = onLike
	}

	/** The post handed over, when it is the one with [postId]. */
	fun postFor(postId: String): FeedContentItem? = post?.takeIf { it.id == postId }

	/** The like handler of the list the post came from, when the post is the one with [postId]. */
	fun likeHandlerFor(postId: String): ((String) -> Unit)? = onLike.takeIf { post?.id == postId }

	/** Called when the detail screen is left, so a later visit never shows a stale post or a stale like. */
	fun clear() {
		post = null
		onLike = null
	}

	/** The handler holds a view model, which must not outlive the screen that owns this one. */
	fun dispose() = clear()

	override fun onCleared() = dispose()
}
