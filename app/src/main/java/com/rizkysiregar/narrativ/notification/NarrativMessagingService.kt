package com.rizkysiregar.narrativ.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.rizkysiregar.narrativ.data.remote.DeviceSyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Re-syncs the device when FCM rotates the push token. */
@AndroidEntryPoint
class NarrativMessagingService : FirebaseMessagingService() {

	@Inject
	lateinit var deviceSyncScheduler: DeviceSyncScheduler

	override fun onNewToken(token: String) {
		deviceSyncScheduler.schedule()
	}
}
