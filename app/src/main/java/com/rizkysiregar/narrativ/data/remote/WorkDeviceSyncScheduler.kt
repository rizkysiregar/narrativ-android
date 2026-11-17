package com.rizkysiregar.narrativ.data.remote

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

/** [DeviceSyncScheduler] on WorkManager. */
@Singleton
class WorkDeviceSyncScheduler @Inject constructor(
	@ApplicationContext private val context: Context
) : DeviceSyncScheduler {

	override fun schedule(force: Boolean) {
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
