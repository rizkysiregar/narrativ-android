package com.rizkysiregar.narrativ.data.remote.upload

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.domain.post.UploadManager
import com.rizkysiregar.narrativ.domain.post.WorkUploadManager
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.testing.fakes.FakeAppAnalytics
import com.rizkysiregar.narrativ.testing.fakes.FakeAppStrings
import com.rizkysiregar.narrativ.testing.fakes.FakeCrashReporter
import com.rizkysiregar.narrativ.testing.fakes.FakePerfTracer
import com.rizkysiregar.narrativ.testing.fakes.FakePostRepository
import com.rizkysiregar.narrativ.testing.httpException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/** The whole upload pipeline of a post: upload URLs, PUTs to storage and the final POST /posts, with its retries. */
// A plain Application: NarrativApplication would start Hilt and Firebase, which this test replaces with fakes.
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class PostUploadWorkerTest {

	@get:Rule
	val folder = TemporaryFolder()

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val strings = FakeAppStrings()
	private val repository = FakePostRepository()
	private val analytics = FakeAppAnalytics()
	private val crashReporter = FakeCrashReporter()
	private lateinit var uploadManager: UploadManager
	private lateinit var server: MockWebServer
	private val puts = CopyOnWriteArrayList<String>()
	private var storageAnswer: (RecordedRequest) -> MockResponse = { MockResponse.Builder().code(200).build() }

	@Before
	fun setUp() {
		WorkManagerTestInitHelper.initializeTestWorkManager(context)
		uploadManager = WorkUploadManager(context, strings)
		server = MockWebServer().apply {
			dispatcher = object : Dispatcher() {
				override fun dispatch(request: RecordedRequest): MockResponse {
					puts += request.url.encodedPath
					return storageAnswer(request)
				}
			}
			start()
		}
		repository.onCreateUploadUrl = { n -> com.rizkysiregar.narrativ.data.remote.model.response.post.CreatePostUploadUrlResponse(server.url("/storage/$n").toString(), "key-$n", 900) }
	}

	@After
	fun tearDown() = server.close()

	private fun media(vararg names: String): List<File> = names.map { folder.newFile(it).apply { writeText("data of $it") } }

	private fun worker(
		files: List<File>,
		contentTypes: List<String> = files.map { "image/jpeg" },
		caption: String = "halo",
		attempt: Int = 0
	): PostUploadWorker {
		val input: Data = workDataOf(
			PostUploadWorker.KEY_FILE_PATHS to files.map { it.absolutePath }.toTypedArray(),
			PostUploadWorker.KEY_CONTENT_TYPES to contentTypes.toTypedArray(),
			PostUploadWorker.KEY_CAPTION to caption
		)
		return TestListenableWorkerBuilder<PostUploadWorker>(context)
			.setInputData(input)
			.setRunAttemptCount(attempt)
			.setWorkerFactory(object : WorkerFactory() {
				override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
					PostUploadWorker(
						appContext, workerParameters, repository, PresignedUploader(OkHttpClient(), Dispatchers.IO),
						uploadManager, analytics, crashReporter, FakePerfTracer(), strings
					)
			})
			.build()
	}

	private fun run(worker: PostUploadWorker): Result = runBlocking { worker.doWork() }

	private fun error(result: Result) = (result as Result.Failure).outputData.getString(PostUploadWorker.KEY_ERROR)

	private fun assertFailed(result: Result, message: String) {
		assertThat(result).isInstanceOf(Result.Failure::class.java)
		assertThat(error(result)).isEqualTo(message)
	}

	// --- the happy paths ---

	@Test
	fun aCaptionOnlyPostSkipsStorageAndIsCreatedWithNoKeys() {
		val result = run(worker(files = emptyList(), caption = "hanya teks"))

		assertThat(result).isEqualTo(Result.success())
		assertThat(puts).isEmpty()
		assertThat(repository.uploadUrlRequests).isEmpty()
		assertThat(repository.createdPosts).containsExactly("hanya teks" to emptyList<String>())
		assertThat(analytics.events.single().name).isEqualTo(AnalyticsEvents.POST_UPLOAD_SUCCEEDED)
	}

	@Test
	fun severalMediaAreUploadedInOrderAndTheKeysKeepThatOrder() {
		val files = media("a.jpg", "b.mp4", "c.png")

		val result = run(worker(files, contentTypes = listOf("image/jpeg", "video/mp4", "image/png")))

		assertThat(result).isEqualTo(Result.success())
		assertThat(repository.uploadUrlRequests).containsExactly("image/jpeg", "video/mp4", "image/png").inOrder()
		assertThat(puts).containsExactly("/storage/0", "/storage/1", "/storage/2").inOrder()
		assertThat(repository.createdPosts.single().second).containsExactly("key-0", "key-1", "key-2").inOrder()
		assertThat(analytics.events.single().params[AnalyticsEvents.Param.MEDIA_COUNT]).isEqualTo(3)
	}

	@Test
	fun afterSuccessTheLocalFilesAreDeletedAndTheAppIsTold() {
		val files = media("a.jpg", "b.jpg")
		val completed = CopyOnWriteArrayList<Unit>()

		runBlocking {
			val job = launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { uploadManager.completions.collect { completed += it } }
			worker(files).doWork()
			job.cancel()
		}

		assertThat(files.none { it.exists() }).isTrue()
		assertThat(completed).hasSize(1)
	}

	@Test
	fun anExpiredUploadUrlIsRequestedAgainOnce() {
		val files = media("a.jpg")
		storageAnswer = { r -> if (r.url.encodedPath == "/storage/0") MockResponse.Builder().code(403).build() else MockResponse.Builder().code(200).build() }

		val result = run(worker(files))

		assertThat(result).isEqualTo(Result.success())
		assertThat(repository.uploadUrlRequests).hasSize(2)
		assertThat(repository.createdPosts.single().second).containsExactly("key-1")
	}

	// --- failures that end the work ---

	@Test
	fun aSecondExpiredUrlFailsTheUploadAndRecordsWhereItBroke() {
		val files = media("a.jpg")
		storageAnswer = { MockResponse.Builder().code(403).build() }

		val result = run(worker(files))

		assertFailed(result, strings.get(R.string.upload_err_media_failed, 403))
		assertThat(repository.uploadUrlRequests).hasSize(2) // asked again once, not forever
		assertThat(repository.createdPosts).isEmpty()
		assertThat(files.all { it.exists() }).isTrue() // kept: nothing was sent
		val failed = analytics.events.single()
		assertThat(failed.name).isEqualTo(AnalyticsEvents.POST_UPLOAD_FAILED)
		assertThat(failed.params[AnalyticsEvents.Param.STAGE]).isEqualTo("storage")
		assertThat(failed.params[AnalyticsEvents.Param.ERROR_CODE]).isEqualTo(403)
		assertThat(crashReporter.nonFatals.single().keys["stage"]).isEqualTo("storage")
	}

	@Test
	fun aRateLimitFailsWithoutRetrying() {
		repository.onCreatePost = { _, _ -> throw httpException(429) }

		val result = run(worker(files = emptyList(), attempt = 0))

		assertFailed(result, strings.get(R.string.upload_err_rate_limit))
	}

	@Test
	fun anOtherClientErrorFailsWithItsCode() {
		repository.onCreatePost = { _, _ -> throw httpException(400) }

		val result = run(worker(files = emptyList()))

		assertFailed(result, strings.get(R.string.upload_err_rejected, 400))
		assertThat(analytics.events.single().params[AnalyticsEvents.Param.STAGE]).isEqualTo("api")
	}

	@Test
	fun theWorkFailsAtOnceWhenTheFilesAreMissing() {
		val ghost = File(folder.root, "gone.jpg")

		val result = run(worker(listOf(ghost)))

		assertFailed(result, strings.get(R.string.upload_err_file_missing))
		assertThat(repository.uploadUrlRequests).isEmpty()
	}

	@Test
	fun theWorkFailsAtOnceWhenTheContentTypesDoNotMatchTheFiles() {
		val result = run(worker(media("a.jpg", "b.jpg"), contentTypes = listOf("image/jpeg")))

		assertFailed(result, strings.get(R.string.upload_err_unknown_media))
	}

	@Test
	fun anEmptyPostIsRefused() {
		val result = run(worker(files = emptyList(), caption = "   "))

		assertFailed(result, strings.get(R.string.upload_err_empty_post))
		assertThat(repository.createdPosts).isEmpty()
	}

	@Test
	fun anUnexpectedErrorFailsWithItsMessageAndIsRecorded() {
		repository.onCreatePost = { _, _ -> throw IllegalStateException("kaboom") }

		val result = run(worker(files = emptyList()))

		assertFailed(result, "kaboom")
		assertThat(crashReporter.nonFatals.single().keys["stage"]).isEqualTo("unexpected")
	}

	// --- failures that are retried ---

	@Test
	fun aServerErrorIsRetriedAtFirst() {
		repository.onCreatePost = { _, _ -> throw httpException(503) }

		val result = run(worker(files = emptyList(), attempt = 0))

		assertThat(result).isEqualTo(Result.retry())
		assertThat(analytics.events).isEmpty() // not final, so not counted as a failure yet
	}

	@Test
	fun aServerErrorGivesUpAfterTheLastAttempt() {
		repository.onCreatePost = { _, _ -> throw httpException(503) }

		val result = run(worker(files = emptyList(), attempt = 3))

		assertFailed(result, strings.get(R.string.upload_err_server))
		assertThat(analytics.events.single().params[AnalyticsEvents.Param.STAGE]).isEqualTo("api")
	}

	@Test
	fun aNetworkErrorIsRetriedThenGivesUp() {
		repository.onCreatePost = { _, _ -> throw IOException("no route") }

		assertThat(run(worker(files = emptyList(), attempt = 2))).isEqualTo(Result.retry())
		assertFailed(run(worker(files = emptyList(), attempt = 3)), strings.get(R.string.upload_err_connection))
	}
}
