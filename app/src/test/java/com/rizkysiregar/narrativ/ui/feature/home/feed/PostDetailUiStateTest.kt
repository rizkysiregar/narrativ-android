package com.rizkysiregar.narrativ.ui.feature.home.feed

import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class PostDetailUiStateTest {

	private fun post(moderationStatus: String? = null, caption: String? = "hello") = FeedContentItem(
		createdAt = "2026-11-17T10:00:00",
		caption = caption,
		likeCount = 2,
		id = "p1",
		liked = false,
		username = "ani",
		commentCount = 1,
		moderationStatus = moderationStatus
	)

	private fun httpError(code: Int) = HttpException(
		Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType()))
	)

	@Test
	fun aLoadedPostIsShown() {
		val post = post()

		assertEquals(PostDetailUiState.Loaded(post), PostDetailUiState.from(ResultState.Success(post)))
	}

	@Test
	fun aCaptionOnlyPostIsShownLikeAnyOther() {
		val state = PostDetailUiState.from(ResultState.Success(post(caption = "just words")))

		assertTrue(state is PostDetailUiState.Loaded)
		assertTrue((state as PostDetailUiState.Loaded).post.mediaItems.isEmpty())
	}

	@Test
	fun aTakenDownPostIsUnavailable() {
		val state = PostDetailUiState.from(ResultState.Success(post(moderationStatus = "TAKEN_DOWN")))

		assertEquals(PostDetailUiState.Unavailable, state)
	}

	@Test
	fun aNotFoundAnswerIsUnavailableAndNotRetryable() {
		val state = PostDetailUiState.from(ResultState.Failure("gone", httpError(404)))

		assertEquals(PostDetailUiState.Unavailable, state)
	}

	@Test
	fun otherFailuresCanBeRetried() {
		assertEquals(
			PostDetailUiState.Error("server down"),
			PostDetailUiState.from(ResultState.Failure("server down", httpError(500)))
		)
		assertEquals(
			PostDetailUiState.Error("offline"),
			PostDetailUiState.from(ResultState.Failure("offline", IOException("no network")))
		)
	}

	@Test
	fun loadingAndIdleShowTheSpinner() {
		assertEquals(PostDetailUiState.Loading, PostDetailUiState.from(ResultState.Loading))
		assertEquals(PostDetailUiState.Loading, PostDetailUiState.from(ResultState.Idle))
	}
}
