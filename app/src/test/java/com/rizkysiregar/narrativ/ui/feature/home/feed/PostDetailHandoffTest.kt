package com.rizkysiregar.narrativ.ui.feature.home.feed

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.testing.aFeedItem
import org.junit.Test

class PostDetailHandoffTest {

	private val handoff = PostDetailHandoff()

	@Test
	fun theDetailGetsThePostThatWasHandedOverAndTheLikeHandlerOfItsList() {
		val post = aFeedItem()
		val liked = mutableListOf<String>()
		handoff.hand(post) { liked += it }

		assertThat(handoff.postFor(post.id)).isEqualTo(post)
		handoff.likeHandlerFor(post.id)!!.invoke(post.id)

		assertThat(liked).containsExactly(post.id)
	}

	@Test
	fun anotherPostGetsNothingSoItIsLoadedByItsId() {
		handoff.hand(aFeedItem()) {}
		val other = aFeedItem()

		assertThat(handoff.postFor(other.id)).isNull()
		assertThat(handoff.likeHandlerFor(other.id)).isNull()
	}

	@Test
	fun nothingHandedOverMeansNothingToGive() {
		assertThat(handoff.postFor("x")).isNull()
		assertThat(handoff.likeHandlerFor("x")).isNull()
	}

	@Test
	fun theNextPostReplacesTheLastOne() {
		val first = aFeedItem()
		val second = aFeedItem()
		handoff.hand(first) {}
		handoff.hand(second) {}

		assertThat(handoff.postFor(first.id)).isNull()
		assertThat(handoff.postFor(second.id)).isEqualTo(second)
	}

	@Test
	fun clearingForgetsBothSoAStalePostIsNeverShownAgain() {
		val post = aFeedItem()
		handoff.hand(post) {}

		handoff.clear()

		assertThat(handoff.postFor(post.id)).isNull()
		assertThat(handoff.likeHandlerFor(post.id)).isNull()
	}

	/** The handler holds a view model: it must not outlive the screen that owns this one. */
	@Test
	fun whenTheOwnerIsClearedTheHandlerIsDropped() {
		val post = aFeedItem()
		handoff.hand(post) {}

		handoff.dispose()

		assertThat(handoff.likeHandlerFor(post.id)).isNull()
	}
}
