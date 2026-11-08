package com.rizkysiregar.feedzmore.data.remote

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rizkysiregar.feedzmore.data.repository.DeviceRegistrar
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Silently syncs device info to the backend. Retries with backoff while the network or
 * server is unavailable, then gives up; the next app start schedules it again.
 */
@HiltWorker
class DeviceSyncWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val deviceRegistrar: DeviceRegistrar
) : CoroutineWorker(appContext, params) {

	override suspend fun doWork(): Result {
		val force = inputData.getBoolean(KEY_FORCE, false)
		Log.d("DeviceSyncWorker", "doWork force=$force attempt=$runAttemptCount")
		return when {
			deviceRegistrar.sync(force) -> Result.success()
			runAttemptCount < (if (force) MAX_FORCED_ATTEMPTS else MAX_ATTEMPTS) -> Result.retry()
			else -> Result.failure()
		}
	}

	companion object {
		const val KEY_FORCE = "force"
		private const val MAX_ATTEMPTS = 5
		private const val MAX_FORCED_ATTEMPTS = 10
	}
}
