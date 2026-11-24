package com.rizkysiregar.narrativ.notification

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rizkysiregar.marpokat.android.MarpokatPush
import com.rizkysiregar.narrativ.chat.ChatSession
import com.rizkysiregar.narrativ.config.FeatureFlags
import com.rizkysiregar.narrativ.data.remote.DeviceSyncScheduler
import com.rizkysiregar.narrativ.domain.notification.UnreadNotifications
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Re-syncs the device when FCM rotates the push token, and shows pushes that arrive while the app is open. */
@AndroidEntryPoint
class NarrativMessagingService : FirebaseMessagingService() {

	@Inject
	lateinit var deviceSyncScheduler: DeviceSyncScheduler

	@Inject
	lateinit var unreadNotifications: UnreadNotifications

	@Inject
	lateinit var chatSession: ChatSession

	@Inject
	lateinit var featureFlags: FeatureFlags

	override fun onNewToken(token: String) {
		deviceSyncScheduler.schedule()
		chatSession.onPushToken(token)
	}

	// Only called for a notification message while the app is in the foreground; in the background FCM
	// shows it by itself.
	override fun onMessageReceived(message: RemoteMessage) {
		// a chat message (Marpokat sends data messages, so this runs in the background too)
		if (MarpokatPush.isMarpokat(message.data)) {
			// an admin switched chat off: the push token is removed then, this catches what is still on its way
			if (!featureFlags.chatEnabled.value) return
			val push = MarpokatPush.parse(message.data) ?: return
			val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
			if (PushNotifications.shouldShowChat(push.channelId, chatSession.visibleChannelId, inForeground)) {
				PushNotifications.showChat(this, push, message.data)
			}
			return
		}
		// a like, comment or follow just landed in the inbox: the bell shows it while the app is open
		unreadNotifications.refresh()
		val notification = message.notification ?: return
		PushNotifications.show(this, notification.title, notification.body, message.data)
	}
}
