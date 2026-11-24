package com.rizkysiregar.narrativ.notification

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The sound of a chat message that arrives while the app is open (the banner: no notification is posted then). */
interface ChatSound {
	fun play()
}

/**
 * Plays Narrativ's notification sound the way the chat notification channel would: not in silent or vibrate mode, not
 * under Do Not Disturb, not when the user switched the chat channel off or made it silent. Messages in a burst make
 * one sound.
 */
@Singleton
class ChannelChatSound @Inject constructor(@ApplicationContext private val context: Context) : ChatSound {
	private var last = 0L
	private var ringtone: Ringtone? = null

	override fun play() {
		val nowMs = SystemClock.elapsedRealtime()
		if (nowMs - last < BURST_MS || !allowed()) return
		last = nowMs
		runCatching {
			ringtone?.stop()
			ringtone = RingtoneManager.getRingtone(context, PushNotifications.soundUri(context))?.apply {
				audioAttributes = AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build()
				play()
			}
		}
	}

	private fun allowed(): Boolean {
		val audio = context.getSystemService(AudioManager::class.java) ?: return false
		if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return false
		val notifications = context.getSystemService(NotificationManager::class.java) ?: return false
		if (notifications.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL) return false
		val channel = notifications.getNotificationChannel(PushNotifications.CHANNEL_CHAT) ?: return true
		return channel.importance > NotificationManager.IMPORTANCE_LOW && channel.sound != null
	}

	private companion object {
		const val BURST_MS = 1_500L
	}
}
