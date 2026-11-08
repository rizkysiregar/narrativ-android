package com.rizkysiregar.feedzmore.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.rizkysiregar.feedzmore.data.remote.DeviceSyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Re-syncs the device when FCM rotates the push token. */
@AndroidEntryPoint
class FeedzmoreMessagingService : FirebaseMessagingService() {

	@Inject
	lateinit var deviceSyncScheduler: DeviceSyncScheduler

	override fun onNewToken(token: String) {
		deviceSyncScheduler.schedule()
	}
}
