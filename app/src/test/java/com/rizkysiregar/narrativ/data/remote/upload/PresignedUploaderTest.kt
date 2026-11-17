package com.rizkysiregar.narrativ.data.remote.upload

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class PresignedUploaderTest {

	@get:Rule
	val folder = TemporaryFolder()

	private lateinit var server: MockWebServer

	@Before
	fun setUp() {
		server = MockWebServer().apply { start() }
	}

	@After
	fun tearDown() = server.close()

	private fun TestScope.uploader() = PresignedUploader(OkHttpClient(), StandardTestDispatcher(testScheduler))

	private fun file(size: Int) = folder.newFile().apply { writeBytes(ByteArray(size) { (it % 251).toByte() }) }

	private fun serve(handler: (RecordedRequest) -> MockResponse) {
		server.dispatcher = object : Dispatcher() {
			override fun dispatch(request: RecordedRequest) = handler(request)
		}
	}

	private fun ok() = MockResponse.Builder().code(200).build()

	private fun drop() = MockResponse.Builder().onResponseStart(SocketEffect.ShutdownConnection).build()

	@Test
	fun theFileIsPutWithTheExactContentTypeThatWasSigned() = runTest {
		serve { ok() }
		val source = file(10_000)

		uploader().upload(server.url("/bucket/a.jpg").toString(), source, "image/jpeg") {}

		val request = server.takeRequest()
		assertThat(request.method).isEqualTo("PUT")
		assertThat(request.url.encodedPath).isEqualTo("/bucket/a.jpg")
		assertThat(request.headers["Content-Type"]).isEqualTo("image/jpeg")
		assertThat(request.body!!.toByteArray()).isEqualTo(source.readBytes())
	}

	@Test
	fun progressRisesToOneAndNeverGoesBack() = runTest {
		serve { ok() }
		val seen = mutableListOf<Float>()

		uploader().upload(server.url("/x").toString(), file(300_000), "video/mp4") { seen += it }

		assertThat(seen).isNotEmpty()
		assertThat(seen.last()).isEqualTo(1f)
		assertThat(seen).isInOrder()
		assertThat(seen.all { it in 0f..1f }).isTrue()
	}

	@Test
	fun aRejectedUploadThrowsWithTheStatusAndRetryAfter() = runTest {
		serve { MockResponse.Builder().code(429).addHeader("Retry-After", "30").build() }

		val error = runCatching { uploader().upload(server.url("/x").toString(), file(100), "image/png") {} }
			.exceptionOrNull() as UploadHttpException

		assertThat(error.code).isEqualTo(429)
		assertThat(error.retryAfterSeconds).isEqualTo(30L)
		assertThat(server.requestCount).isEqualTo(1) // an answer from the server is never retried here
	}

	@Test
	fun anExpiredSignatureIsReportedAs403() = runTest {
		serve { MockResponse.Builder().code(403).build() }

		val error = runCatching { uploader().upload(server.url("/x").toString(), file(100), "image/png") {} }
			.exceptionOrNull() as UploadHttpException

		assertThat(error.code).isEqualTo(403)
		assertThat(error.retryAfterSeconds).isNull()
	}

	@Test
	fun aDroppedConnectionIsRetriedAndTheUploadCanStillSucceed() = runTest {
		val calls = AtomicInteger()
		serve { if (calls.incrementAndGet() == 1) drop() else ok() }

		uploader().upload(server.url("/x").toString(), file(5_000), "image/jpeg") {}

		assertThat(calls.get()).isAtLeast(2)
	}

	@Test
	fun aConnectionThatKeepsFailingGivesUpWithTheNetworkError() = runTest {
		serve { drop() }

		assertThrows(IOException::class.java) {
			kotlinx.coroutines.runBlocking {
				PresignedUploader(OkHttpClient(), kotlinx.coroutines.Dispatchers.IO)
					.upload(server.url("/x").toString(), file(5_000), "image/jpeg") {}
			}
		}

		assertThat(server.requestCount).isAtLeast(3) // the first try and the two retries
	}
}
