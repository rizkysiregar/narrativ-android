package com.rizkysiregar.narrativ.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.graphics.toArgb
import com.rizkysiregar.narrativ.MainActivity
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.ui.theme.Accent500
import com.rizkysiregar.narrativ.ui.theme.Error
import com.rizkysiregar.narrativ.ui.theme.Success
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import com.rizkysiregar.narrativ.i18n.AppStrings

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Builds and posts notifications for the background post upload flow.
 */
object UploadNotifications {

	const val CHANNEL_PROGRESS = "upload_progress"
	const val CHANNEL_RESULT = "upload_result"

	private const val MIN_PERCENT_STEP = 2

	// last stage and percent posted per progress notification, to skip updates nobody would see
	private val lastPosted = ConcurrentHashMap<Int, Pair<String, Int>>()

	fun createChannels(appContext: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
		val context = AppStrings.localized(appContext)
		val manager = context.getSystemService(NotificationManager::class.java) ?: return

		manager.createNotificationChannel(
			NotificationChannel(
				CHANNEL_PROGRESS,
				context.getString(R.string.notif_channel_progress),
				NotificationManager.IMPORTANCE_LOW
			).apply {
				description = context.getString(R.string.notif_channel_progress_desc)
				setShowBadge(false)
			}
		)

		manager.createNotificationChannel(
			NotificationChannel(
				CHANNEL_RESULT,
				context.getString(R.string.notif_channel_result),
				NotificationManager.IMPORTANCE_DEFAULT
			).apply {
				description = context.getString(R.string.notif_channel_result_desc)
			}
		)
	}

	fun buildProgress(
		context: Context,
		caption: String,
		stageLabel: String,
		percent: Int
	): Notification {
		val titleProgress = AppStrings.localized(context).getString(R.string.notif_sending)
		val progress = percent.coerceIn(0, PROGRESS_MAX)
		val percentText = "$progress%"

		val collapsed = RemoteViews(context.packageName, R.layout.notification_upload_collapsed).apply {
			setTextViewText(R.id.title, titleProgress)
			setTextViewText(R.id.percent, percentText)
			setImageViewBitmap(R.id.bar, UploadBarRenderer.render(progress, UploadBarRenderer.HEIGHT_COLLAPSED_PX))
		}
		val expanded = RemoteViews(context.packageName, R.layout.notification_upload_expanded).apply {
			setTextViewText(R.id.title, titleProgress)
			setTextViewText(R.id.percent, percentText)
			setTextViewText(R.id.caption, caption.ifBlank { stageLabel })
			setViewVisibility(R.id.caption, if (caption.isBlank()) View.GONE else View.VISIBLE)
			setImageViewBitmap(R.id.bar, UploadBarRenderer.render(progress, UploadBarRenderer.HEIGHT_EXPANDED_PX))
			setTextViewText(R.id.stage, stageLabel)
		}

		return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
			.setSmallIcon(R.drawable.ic_stat_narrativ)
			.setColor(Accent500.toArgb())
			// the plain fields stay filled for surfaces that do not draw custom views (lock screen, wearables)
			.setContentTitle(titleProgress)
			.setContentText(caption.ifBlank { stageLabel })
			.setStyle(NotificationCompat.DecoratedCustomViewStyle())
			.setCustomContentView(collapsed)
			.setCustomBigContentView(expanded)
			.setOngoing(true)
			.setOnlyAlertOnce(true)
			.setContentIntent(contentIntent(context))
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
			.build()
	}

	fun buildResult(context: Context, caption: String, success: Boolean): Notification {
		val strings = AppStrings.localized(context)
		val title = strings.getString(if (success) R.string.notif_posted else R.string.notif_failed)
		val text = caption.ifBlank { strings.getString(if (success) R.string.notif_live else R.string.notif_try_again) }

		val card = RemoteViews(context.packageName, R.layout.notification_upload_result).apply {
			setImageViewResource(R.id.badge, if (success) R.drawable.ic_notif_success else R.drawable.ic_notif_error)
			setTextViewText(R.id.title, title)
			setTextViewText(R.id.text, text)
		}

		return NotificationCompat.Builder(context, CHANNEL_RESULT)
			.setSmallIcon(R.drawable.ic_stat_narrativ)
			.setColor(if (success) Success.toArgb() else Error.toArgb())
			.setContentTitle(title)
			.setContentText(text)
			.setStyle(NotificationCompat.DecoratedCustomViewStyle())
			.setCustomContentView(card)
			.setAutoCancel(true)
			.setContentIntent(contentIntent(context))
			.build()
	}

	/**
	 * Progress ticks can arrive many times per percent, and each one re-renders the bar bitmap and
	 * sends it to the system, so only post when the stage changed or the bar moved a visible step.
	 */
	fun notifyProgress(
		context: Context,
		notificationId: Int,
		caption: String,
		stageLabel: String,
		percent: Int
	) {
		val last = lastPosted[notificationId]
		val unchanged = last != null && last.first == stageLabel &&
			abs(percent - last.second) < MIN_PERCENT_STEP && !(percent == PROGRESS_MAX && last.second != PROGRESS_MAX)
		if (unchanged) return
		lastPosted[notificationId] = stageLabel to percent
		post(context, notificationId, buildProgress(context, caption, stageLabel, percent))
	}

	fun notifyResult(
		context: Context,
		notificationId: Int,
		progressNotificationId: Int,
		caption: String,
		success: Boolean
	) {
		lastPosted.remove(progressNotificationId)
		post(context, notificationId, buildResult(context, caption, success))
	}

	// areNotificationsEnabled() is false without POST_NOTIFICATIONS on API 33+, which is the permission check lint wants.
	@SuppressLint("MissingPermission")
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
