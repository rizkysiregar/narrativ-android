package com.rizkysiregar.narrativ.ui.feature.home.post

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.narrativ.domain.post.MediaValidator
import com.rizkysiregar.narrativ.domain.profile.FetchUserProfileUseCase
import com.rizkysiregar.narrativ.media.MediaValidationException
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.testing.MainDispatcherRule
import com.rizkysiregar.narrativ.testing.fakes.FakeAppAnalytics
import com.rizkysiregar.narrativ.testing.fakes.FakeAppStrings
import com.rizkysiregar.narrativ.testing.fakes.FakeMediaInspector
import com.rizkysiregar.narrativ.testing.fakes.FakeMediaPreparation
import com.rizkysiregar.narrativ.testing.fakes.FakeUploadManager
import com.rizkysiregar.narrativ.testing.fakes.FakeUserRepository
import com.rizkysiregar.narrativ.testing.fakes.successOf
import com.rizkysiregar.narrativ.testing.preferencesFile
import com.rizkysiregar.narrativ.testing.testDataStoreManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric only to create a Uri.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class PostFeedViewModelTest {

	@get:Rule
	val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

	@get:Rule
	val folder = TemporaryFolder()

	private val inspector = FakeMediaInspector()
	private val preparation = FakeMediaPreparation()
	private val uploads = FakeUploadManager()
	private val users = FakeUserRepository()
	private val analytics = FakeAppAnalytics()
	private val strings = FakeAppStrings()

	private fun uri(n: Int): Uri = Uri.parse("content://media/$n")

	private fun image(n: Int): Uri = uri(n).also { inspector.describe(it, "image/jpeg", sizeBytes = 100) }

	private fun video(n: Int): Uri = uri(n).also { inspector.describe(it, "video/mp4", sizeBytes = 1000, durationMs = 5_000) }

	private suspend fun TestScope.viewModel(userId: String = ""): PostFeedViewModel {
		val dataStore = testDataStoreManager(backgroundScope, folder.preferencesFile())
		dataStore.put(PrefKeys.userId, userId)
		return PostFeedViewModel(
			MediaValidator(inspector), preparation, uploads, dataStore, FetchUserProfileUseCase(users), analytics, strings
		)
	}

	// --- the author ---

	@Test
	fun theSignedInUserIsLoadedAsTheAuthor() = runTest(mainRule.dispatcher) {
		val me = UserSearchItemResponse(id = "u1", username = "rizky")
		users.onGetUserProfile = { successOf(me) }

		val vm = viewModel(userId = "u1")

		assertThat(vm.uiState.value.author).isEqualTo(me)
		assertThat(users.profileRequests).containsExactly("u1")
	}

	@Test
	fun withoutASignedInUserNoProfileIsRequested() = runTest(mainRule.dispatcher) {
		val vm = viewModel(userId = "")

		assertThat(vm.uiState.value.author).isNull()
		assertThat(users.profileRequests).isEmpty()
	}

	// --- picking ---

	@Test
	fun pickedMediaIsAddedInOrderWithItsType() = runTest(mainRule.dispatcher) {
		val vm = viewModel()
		val a = image(1)
		val b = video(2)

		vm.onMediaPicked(listOf(a, b))

		assertThat(vm.uiState.value.selectedMedia.map { it.uri }).containsExactly(a, b).inOrder()
		assertThat(vm.uiState.value.selectedMedia.map { it.mimeType }).containsExactly("image/jpeg", "video/mp4").inOrder()
		assertThat(vm.uiState.value.validationError).isNull()
	}

	@Test
	fun theSameMediaIsNotAddedTwice() = runTest(mainRule.dispatcher) {
		val vm = viewModel()
		val a = image(1)

		vm.onMediaPicked(listOf(a, a))
		vm.onMediaPicked(listOf(a))

		assertThat(vm.uiState.value.selectedMedia).hasSize(1)
	}

	@Test
	fun theSixthMediaIsRefusedWithTheLimitAndTheFirstFiveStay() = runTest(mainRule.dispatcher) {
		val vm = viewModel()

		vm.onMediaPicked((1..6).map { image(it) })

		assertThat(vm.uiState.value.selectedMedia).hasSize(MAX_POST_MEDIA)
		assertThat(vm.uiState.value.validationError).isEqualTo(strings.get(R.string.post_err_max_media, MAX_POST_MEDIA))
	}

	@Test
	fun anInvalidMediaIsSkippedWithItsReasonAndTheValidOnesAreKept() = runTest(mainRule.dispatcher) {
		val vm = viewModel()
		val good = image(1)
		val bad = uri(2).also { inspector.describe(it, "audio/mpeg") }

		vm.onMediaPicked(listOf(bad, good))

		assertThat(vm.uiState.value.selectedMedia.map { it.uri }).containsExactly(good)
		assertThat(vm.uiState.value.validationError).isEqualTo(strings.get(R.string.media_err_format))
	}

	@Test
	fun aLaterValidPickClearsTheEarlierError() = runTest(mainRule.dispatcher) {
		val vm = viewModel()
		vm.onMediaPicked(listOf(uri(9).also { inspector.describe(it, null) }))
		assertThat(vm.uiState.value.validationError).isNotNull()

		vm.onMediaPicked(listOf(image(1)))

		assertThat(vm.uiState.value.validationError).isNull()
	}

	@Test
	fun removingMediaKeepsTheRestInOrderAndIgnoresABadIndex() = runTest(mainRule.dispatcher) {
		val vm = viewModel()
		val (a, b, c) = listOf(image(1), image(2), image(3))
		vm.onMediaPicked(listOf(a, b, c))

		vm.onMediaRemoved(1)
		vm.onMediaRemoved(7)
		vm.onMediaRemoved(-1)

		assertThat(vm.uiState.value.selectedMedia.map { it.uri }).containsExactly(a, c).inOrder()
	}

	@Test
	fun theCaptionIsCutAtTheLimit() = runTest(mainRule.dispatcher) {
		val vm = viewModel()

		vm.onCaptionChanged("x".repeat(MAX_CAPTION_LENGTH + 50))

		assertThat(vm.uiState.value.caption).hasLength(MAX_CAPTION_LENGTH)
	}

	// --- submitting ---

	@Test
	fun anEmptyPostIsNotSubmitted() = runTest(mainRule.dispatcher) {
		val vm = viewModel()

		vm.submitPost()

		assertThat(uploads.enqueued).isEmpty()
		assertThat(preparation.prepared).isEmpty()
	}

	@Test
	fun aSubmittedPostIsPreparedHandedToTheUploaderAndTheScreenLeaves() = runTest(mainRule.dispatcher) {
		val vm = viewModel()
		vm.onMediaPicked(listOf(image(1), video(2)))
		vm.onCaptionChanged("halo")

		vm.navigateHome.test {
			vm.submitPost()
			awaitItem()
		}

		assertThat(preparation.prepared.single().map { it.mimeType }).containsExactly("image/jpeg", "video/mp4").inOrder()
		val job = uploads.enqueued.single()
		assertThat(job.caption).isEqualTo("halo")
		assertThat(job.media.map { it.contentType }).containsExactly("image/jpeg", "video/mp4").inOrder()
		val event = analytics.events.single()
		assertThat(event.name).isEqualTo(AnalyticsEvents.POST_SUBMITTED)
		assertThat(event.params[AnalyticsEvents.Param.MEDIA_COUNT]).isEqualTo(2)
		assertThat(event.params[AnalyticsEvents.Param.HAS_VIDEO]).isEqualTo(true)
		assertThat(event.params[AnalyticsEvents.Param.HAS_CAPTION]).isEqualTo(true)
	}

	@Test
	fun aCaptionOnlyPostIsEnqueuedWithoutMedia() = runTest(mainRule.dispatcher) {
		val vm = viewModel()
		vm.onCaptionChanged("hanya teks")

		vm.submitPost()

		val job = uploads.enqueued.single()
		assertThat(job.media).isEmpty()
		assertThat(job.caption).isEqualTo("hanya teks")
		assertThat(analytics.events.single().params[AnalyticsEvents.Param.HAS_VIDEO]).isEqualTo(false)
	}

	@Test
	fun aRejectedMediaShowsItsReasonAndNothingIsUploaded() = runTest(mainRule.dispatcher) {
		val vm = viewModel()
		vm.onMediaPicked(listOf(image(1)))
		preparation.onPrepareAll = { throw MediaValidationException(R.string.media_err_video_size) }

		vm.navigateHome.test {
			vm.submitPost()
			expectNoEvents()
		}

		assertThat(vm.uiState.value.validationError).isEqualTo(strings.get(R.string.media_err_video_size))
		assertThat(uploads.enqueued).isEmpty()
		assertThat(analytics.events).isEmpty()
	}

	@Test
	fun anUnexpectedPreparationErrorShowsTheGenericMessage() = runTest(mainRule.dispatcher) {
		val vm = viewModel()
		vm.onMediaPicked(listOf(image(1)))
		preparation.onPrepareAll = { throw IllegalStateException("disk full") }

		vm.submitPost()

		assertThat(vm.uiState.value.validationError).isEqualTo(strings.get(R.string.post_err_prepare))
		assertThat(uploads.enqueued).isEmpty()
	}

	@Test
	fun theValidationErrorCanBeDismissed() = runTest(mainRule.dispatcher) {
		val vm = viewModel()
		vm.onMediaPicked(listOf(uri(9).also { inspector.describe(it, null) }))

		vm.clearValidationError()

		assertThat(vm.uiState.value.validationError).isNull()
	}
}
