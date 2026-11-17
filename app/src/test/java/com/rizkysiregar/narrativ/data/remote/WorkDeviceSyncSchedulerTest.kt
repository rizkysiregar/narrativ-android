package com.rizkysiregar.narrativ.data.remote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class WorkDeviceSyncSchedulerTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private lateinit var scheduler: WorkDeviceSyncScheduler

	@Before
	fun setUp() {
		WorkManagerTestInitHelper.initializeTestWorkManager(context)
		scheduler = WorkDeviceSyncScheduler(context)
	}

	private fun works(): List<WorkInfo> = WorkManager.getInstance(context).getWorkInfosForUniqueWork("device_sync").get()

	private fun live() = works().filter { !it.state.isFinished }

	@Test
	fun callingItOftenKeepsASinglePendingSync() {
		scheduler.schedule()
		val first = live().single().id

		scheduler.schedule()
		scheduler.schedule()

		assertThat(live().single().id).isEqualTo(first)
	}

	@Test
	fun aForcedSyncReplacesThePendingOne() {
		scheduler.schedule()
		val first = live().single().id

		scheduler.schedule(force = true)

		val all = works()
		assertThat(all).hasSize(1) // the old one is gone, not queued beside it
		assertThat(all.single().id).isNotEqualTo(first)
		assertThat(all.single().state).isEqualTo(WorkInfo.State.ENQUEUED)
	}

	/**
	 * The test WorkManager runs a work at once unless a constraint holds it back; having this one still ENQUEUED
	 * (instead of failed, since no factory can build the worker here) shows the network constraint is set.
	 */
	@Test
	fun itWaitsForANetworkConnection() {
		scheduler.schedule()

		assertThat(live().single().state).isEqualTo(WorkInfo.State.ENQUEUED)
	}
}
