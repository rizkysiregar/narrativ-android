package com.rizkysiregar.narrativ.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rizkysiregar.narrativ.data.remote.DeviceSyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Re-syncs the device when FCM rotates the push token, and shows pushes that arrive while the app is open. */
@AndroidEntryPoint
class NarrativMessagingService : FirebaseMessagingService() {

	@Inject
	lateinit var deviceSyncScheduler: DeviceSyncScheduler

	override fun onNewToken(token: String) {
		deviceSyncScheduler.schedule()
	}

	// Only called for a notification message while the app is in the foreground; in the background FCM
	// shows it by itself.
	override fun onMessageReceived(message: RemoteMessage) {
		val notification = message.notification ?: return
		PushNotifications.show(this, notification.title, notification.body, message.data)
	}
}
