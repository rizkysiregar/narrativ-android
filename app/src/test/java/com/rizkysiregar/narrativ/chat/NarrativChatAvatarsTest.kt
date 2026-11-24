package com.rizkysiregar.narrativ.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class NarrativChatAvatarsTest {
	private var clock = 0L
	private val asked = mutableListOf<String>()
	private var answer: (String) -> String? = { "https://pics.test/$it.jpg" }

	private fun TestScope.avatars() = NarrativChatAvatars(
		fetch = { id -> asked += id; answer(id) },
		scope = backgroundScope,
		now = { clock },
	)

	@Test fun asksOnceAndAgainBeforeThePresignedUrlExpires() = runTest {
		val a = avatars()
		assertThat(a.of("u1")).isEqualTo("https://pics.test/u1.jpg")
		assertThat(a.of("u1")).isEqualTo("https://pics.test/u1.jpg")
		assertThat(asked).containsExactly("u1")
		clock += NarrativChatAvatars.FRESH_MS
		a.of("u1")
		assertThat(asked).containsExactly("u1", "u1")
	}

	@Test fun wantPublishesThePicturesAndSkipsWhatIsKnown() = runTest {
		val a = avatars()
		a.want(listOf("u1", "u2", "u1", ""))
		runCurrent()
		assertThat(a.urls.value).containsExactly("u1", "https://pics.test/u1.jpg", "u2", "https://pics.test/u2.jpg")
		a.want(listOf("u1"))
		runCurrent()
		assertThat(asked).containsExactly("u1", "u2")
	}

	@Test fun noPictureOrAnErrorMeansTheGeneratedAvatar() = runTest {
		answer = { id ->
			when (id) {
				"none" -> null
				"blocked" -> throw HttpException(Response.error<Any>(404, "".toResponseBody()))
				else -> throw IOException("offline")
			}
		}
		val a = avatars()
		assertThat(a.of("none")).isNull()
		assertThat(a.of("blocked")).isNull()
		assertThat(a.of("offline")).isNull()
		assertThat(a.urls.value).isEmpty()
		// offline is asked again soon, a refusal is not
		clock += NarrativChatAvatars.RETRY_MS
		a.of("blocked"); a.of("offline")
		assertThat(asked.count { it == "blocked" }).isEqualTo(1)
		assertThat(asked.count { it == "offline" }).isEqualTo(2)
	}
}
