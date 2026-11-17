package com.rizkysiregar.narrativ.data.remote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.testing.fakes.FakeDeviceRegistrar
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class DeviceSyncWorkerTest {

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val registrar = FakeDeviceRegistrar()

	private fun run(force: Boolean? = null, attempt: Int = 0): Result {
		val builder = TestListenableWorkerBuilder<DeviceSyncWorker>(context)
			.setRunAttemptCount(attempt)
			.setWorkerFactory(object : WorkerFactory() {
				override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
					DeviceSyncWorker(appContext, workerParameters, registrar)
			})
		if (force != null) builder.setInputData(workDataOf(DeviceSyncWorker.KEY_FORCE to force))
		return runBlocking { builder.build().doWork() }
	}

	@Test
	fun aDoneSyncSucceeds() {
		assertThat(run()).isEqualTo(Result.success())
	}

	@Test
	fun theForceFlagIsPassedOn() {
		run(force = true)
		run(force = false)
		run()

		assertThat(registrar.calls).containsExactly(true, false, false).inOrder()
	}

	@Test
	fun aFailedSyncIsRetriedFiveTimesAndThenGivesUp() {
		registrar.result = false

		assertThat(run(attempt = 0)).isEqualTo(Result.retry())
		assertThat(run(attempt = 4)).isEqualTo(Result.retry())
		assertThat(run(attempt = 5)).isEqualTo(Result.failure())
	}

	@Test
	fun aForcedSyncIsMoreStubbornBecauseTheNewSessionNeedsTheDevice() {
		registrar.result = false

		assertThat(run(force = true, attempt = 9)).isEqualTo(Result.retry())
		assertThat(run(force = true, attempt = 10)).isEqualTo(Result.failure())
	}
}
