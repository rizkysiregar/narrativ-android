package com.rizkysiregar.feedzmore.data.remote

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Enqueues the background device sync. Safe to call often: a pending run is kept, not duplicated. */
@Singleton
class DeviceSyncScheduler @Inject constructor(
	@ApplicationContext private val context: Context
) {
	/**
	 * @param force bypass the throttle and replace any pending sync; used right after login,
	 * where the device must be registered for the new session.
	 */
	fun schedule(force: Boolean = false) {
		Log.d("DeviceSyncScheduler", "schedule(force=$force)")
		val request = OneTimeWorkRequestBuilder<DeviceSyncWorker>()
			.setInputData(workDataOf(DeviceSyncWorker.KEY_FORCE to force))
			.setConstraints(
				Constraints.Builder()
					.setRequiredNetworkType(NetworkType.CONNECTED)
					.build()
			)
			.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
			.build()

		WorkManager.getInstance(context)
			.enqueueUniqueWork(
				UNIQUE_NAME,
				if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
				request
			)
	}

	private companion object {
		const val UNIQUE_NAME = "device_sync"
	}
}
