package com.rizkysiregar.feedzmore.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rizkysiregar.feedzmore.MainActivity

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Builds and posts notifications for the background post upload flow.
 */
object UploadNotifications {

	const val CHANNEL_PROGRESS = "upload_progress"
	const val CHANNEL_RESULT = "upload_result"

	fun createChannels(context: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
		val manager = context.getSystemService(NotificationManager::class.java) ?: return

		manager.createNotificationChannel(
			NotificationChannel(
				CHANNEL_PROGRESS,
				"Progres unggahan",
				NotificationManager.IMPORTANCE_LOW
			).apply {
				description = "Menampilkan progres upload post"
				setShowBadge(false)
			}
		)

		manager.createNotificationChannel(
			NotificationChannel(
				CHANNEL_RESULT,
				"Hasil unggahan",
				NotificationManager.IMPORTANCE_DEFAULT
			).apply {
				description = "Hasil akhir upload post"
			}
		)
	}

	fun buildProgress(
		context: Context,
		caption: String,
		stageLabel: String,
		percent: Int
	): Notification =
		NotificationCompat.Builder(context, CHANNEL_PROGRESS)
			.setSmallIcon(android.R.drawable.stat_sys_upload)
			.setContentTitle("Mengunggah post")
			.setContentText(caption.ifBlank { stageLabel })
			.setSubText(stageLabel)
			.setProgress(PROGRESS_MAX, percent.coerceIn(0, PROGRESS_MAX), false)
			.setOngoing(true)
			.setOnlyAlertOnce(true)
			.setContentIntent(contentIntent(context))
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
			.build()

	fun buildResult(context: Context, caption: String, success: Boolean): Notification =
		NotificationCompat.Builder(context, CHANNEL_RESULT)
			.setSmallIcon(
				if (success) android.R.drawable.stat_sys_upload_done
				else android.R.drawable.stat_notify_error
			)
			.setContentTitle(if (success) "Post berhasil diunggah" else "Upload post gagal")
			.setContentText(caption.ifBlank { if (success) "Post kamu sudah tampil di feed" else "Silakan coba lagi" })
			.setAutoCancel(true)
			.setContentIntent(contentIntent(context))
			.build()

	fun notifyProgress(
		context: Context,
		notificationId: Int,
		caption: String,
		stageLabel: String,
		percent: Int
	) {
		post(context, notificationId, buildProgress(context, caption, stageLabel, percent))
	}

	fun notifyResult(context: Context, notificationId: Int, caption: String, success: Boolean) {
		post(context, notificationId, buildResult(context, caption, success))
	}

	private fun post(context: Context, notificationId: Int, notification: Notification) {
		val manager = NotificationManagerCompat.from(context)
		if (!manager.areNotificationsEnabled()) return
		runCatching { manager.notify(notificationId, notification) }
	}

	private fun contentIntent(context: Context): PendingIntent {
		val intent = Intent(context, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
		}
		return PendingIntent.getActivity(
			context,
			0,
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)
	}

	private const val PROGRESS_MAX = 100
}
