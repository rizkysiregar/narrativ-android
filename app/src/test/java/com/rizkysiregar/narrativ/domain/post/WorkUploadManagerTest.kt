package com.rizkysiregar.narrativ.domain.post

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.testing.fakes.FakeAppStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class WorkUploadManagerTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val strings = FakeAppStrings()
	private lateinit var manager: WorkUploadManager

	@Before
	fun setUp() {
		WorkManagerTestInitHelper.initializeTestWorkManager(context)
		manager = WorkUploadManager(context, strings)
	}

	private fun media(vararg paths: String) = paths.map { PreparedMedia(it, "image/jpeg") }

	private fun pending() = runBlocking { manager.pendingUploads.first() }

	private fun scheduled() = WorkManager.getInstance(context).getWorkInfosByTag(WorkUploadManager.TAG).get()

	@Test
	fun aPostNeedsAMediaOrACaption() {
		assertThrows(IllegalArgumentException::class.java) { manager.enqueue(emptyList(), "   ") }

		assertThat(scheduled()).isEmpty()
	}

	@Test
	fun anEnqueuedPostShowsAsPendingWithItsCaptionAndTheWaitingStage() {
		manager.enqueue(media("/a.jpg"), "halo dunia")

		val upload = pending().single()
		assertThat(upload.caption).isEqualTo("halo dunia")
		assertThat(upload.state).isEqualTo(WorkInfo.State.ENQUEUED)
		assertThat(upload.stageLabel).isEqualTo(strings.get(R.string.stage_waiting))
		assertThat(upload.progress).isEqualTo(0)
	}

	@Test
	fun theSameMediaEnqueuedTwiceIsOnePost() {
		manager.enqueue(media("/a.jpg", "/b.jpg"), "satu")
		manager.enqueue(media("/a.jpg", "/b.jpg"), "satu lagi")

		assertThat(scheduled()).hasSize(1)
	}

	@Test
	fun twoIdenticalCaptionOnlyPostsAreBothSent() {
		manager.enqueue(emptyList(), "sama")
		manager.enqueue(emptyList(), "sama")

		assertThat(scheduled()).hasSize(2)
		assertThat(pending().map { it.caption }).containsExactly("sama", "sama")
	}

	@Test
	fun differentMediaMakeDifferentPosts() {
		manager.enqueue(media("/a.jpg"), "x")
		manager.enqueue(media("/b.jpg"), "x")

		assertThat(scheduled()).hasSize(2)
	}

	@Test
	fun aCancelledUploadIsNoLongerPending() {
		manager.enqueue(media("/a.jpg"), "batal")
		val id = pending().single().id

		manager.cancel(id)

		assertThat(pending()).isEmpty()
	}

	@Test
	fun completionsAreAnnouncedToWhoeverIsListening() = runBlocking {
		manager.completions.test {
			manager.notifyUploadCompleted()
			awaitItem()
			expectNoEvents()
		}
	}
}
