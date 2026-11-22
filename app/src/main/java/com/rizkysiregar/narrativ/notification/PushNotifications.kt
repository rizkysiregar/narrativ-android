package com.rizkysiregar.narrativ.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.net.Uri
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rizkysiregar.narrativ.MainActivity
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.i18n.AppStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Push notifications from the server: a new post from someone you follow, and likes, comments and replies on
 * your own posts and comments.
 *
 * While the app is in the background FCM shows the notification itself, using [CHANNEL_GENERAL] (set as the
 * default channel in the manifest). In the foreground FCM shows nothing, so [NarrativMessagingService] calls
 * [show] to post the same notification. Both carry the push's data as intent extras, which [consumeTap] turns
 * into a [PushTarget] once MainActivity opens.
 */
object PushNotifications {

	// A channel's sound and importance are fixed once it exists, so changing either needs a new id; the old
	// ones are deleted. v3 is IMPORTANCE_HIGH, which is what lets a push pop up on screen (heads-up).
	const val CHANNEL_GENERAL = "general_v3"

	/** Chat messages (Marpokat), its own channel so the user can silence chat without silencing likes and comments. */
	const val CHANNEL_CHAT = "chat_v1"
	private val CHANNELS_LEGACY = listOf("general", "general_v2")

	private val VIBRATION_PATTERN = longArrayOf(0, 250, 150, 250)

	private const val KEY_MARPOKAT = "marpokat"
	private const val KEY_CHAT_CHANNEL_ID = "channel_id"
	private const val KEY_TYPE = "type"
	private const val KEY_POST_ID = "postId"
	private const val KEY_COMMENT_ID = "commentId"

	// "someone you follow posted": the person is the post's author
	private const val TYPE_NEW_POST = "NEW_POST"
	private const val KEY_AUTHOR_ID = "authorId"
	private const val KEY_AUTHOR_USERNAME = "authorUsername"

	// activity on your posts and comments (a like, a comment, a reply): the person is whoever did it
	private val COMMENT_TYPES = setOf("POST_COMMENTED", "COMMENT_REPLIED", "COMMENT_PINNED")
	// a follow carries no post, so it always opens the follower's profile
	private val ACTIVITY_TYPES = setOf("POST_LIKED", "FOLLOWED") + COMMENT_TYPES
	private const val KEY_ACTOR_ID = "actorId"
	private const val KEY_ACTOR_USERNAME = "actorUsername"

	/** Where a tapped push wants to go; the home screen takes it and clears it. */
	sealed interface PushTarget {
		/**
		 * The post the push is about; [openComments] when it is about a comment or reply, [commentId] being that
		 * comment.
		 */
		data class Post(val postId: String, val openComments: Boolean, val commentId: String? = null) : PushTarget

		/** The person behind a push that carries no post id (pushes sent before it was added). */
		data class Profile(val userId: String, val username: String) : PushTarget

		/** A conversation: a chat message push (Marpokat). */
		data class Chat(val channelId: String) : PushTarget
	}

	private val pending = MutableStateFlow<PushTarget?>(null)
	val pendingTarget: StateFlow<PushTarget?> = pending.asStateFlow()

	fun clearPendingTarget() {
		pending.value = null
	}

	fun createChannel(appContext: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
		val context = AppStrings.localized(appContext)
		val manager = context.getSystemService(NotificationManager::class.java) ?: return
		CHANNELS_LEGACY.forEach(manager::deleteNotificationChannel)
		manager.createNotificationChannel(
			NotificationChannel(CHANNEL_CHAT, context.getString(R.string.chat_push_channel), NotificationManager.IMPORTANCE_HIGH).apply {
				enableVibration(true)
				vibrationPattern = VIBRATION_PATTERN
			}
		)
		manager.createNotificationChannel(
			NotificationChannel(
				CHANNEL_GENERAL,
				context.getString(R.string.notif_channel_general),
				NotificationManager.IMPORTANCE_HIGH
			).apply {
				description = context.getString(R.string.notif_channel_general_desc)
				enableVibration(true)
				vibrationPattern = VIBRATION_PATTERN
				setSound(
					Uri.parse("android.resource://${context.packageName}/${R.raw.narrativ_notification}"),
					AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_NOTIFICATION)
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
						.build()
				)
			}
		)
	}

	/** Posts a push received while the app is open. Does nothing without the notification permission. */
	fun show(context: Context, title: String?, body: String?, data: Map<String, String>) {
		if (title.isNullOrBlank() && body.isNullOrBlank()) return
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
			ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
			PackageManager.PERMISSION_GRANTED
		) return

		// one id per event, so a like and a comment on the same post show as two notifications
		val id = abs(
			listOf(data[KEY_TYPE], data[KEY_POST_ID], data[KEY_COMMENT_ID], data[KEY_ACTOR_ID], title, body)
				.joinToString("|").hashCode()
		)
		val tapIntent = Intent(context, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
			data.forEach { (key, value) -> putExtra(key, value) }
		}
		val contentIntent = PendingIntent.getActivity(
			context, id, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)
		val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
			.setSmallIcon(R.drawable.ic_stat_narrativ)
			.setColor(ContextCompat.getColor(context, R.color.notif_accent))
			.setContentTitle(title)
			.setContentText(body)
			.setStyle(NotificationCompat.BigTextStyle().bigText(body))
			.setCategory(NotificationCompat.CATEGORY_SOCIAL)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setAutoCancel(true)
			.setContentIntent(contentIntent)
			.build()
		NotificationManagerCompat.from(context).notify(id, notification)
	}

	/**
	 * Posts a chat message push. One notification per conversation: a new message replaces the last one of that
	 * conversation instead of stacking. Does nothing without the notification permission.
	 */
	fun showChat(context: Context, push: com.rizkysiregar.marpokat.android.PushMessage, data: Map<String, String>) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
			ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
		) return
		val localized = AppStrings.localized(context)
		val body = push.text.ifBlank {
			localized.getString(
				when (push.attachmentKind) {
					"image" -> R.string.chat_photo
					"video" -> R.string.chat_video
					else -> R.string.chat_file
				}
			)
		}
		val id = abs(push.channelId.hashCode())
		val tapIntent = Intent(context, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
			data.forEach { (key, value) -> putExtra(key, value) }
		}
		val contentIntent = PendingIntent.getActivity(context, id, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
		val notification = NotificationCompat.Builder(context, CHANNEL_CHAT)
			.setSmallIcon(R.drawable.ic_stat_narrativ)
			.setColor(ContextCompat.getColor(context, R.color.notif_accent))
			.setContentTitle(push.title)
			.setContentText(body)
			.setStyle(NotificationCompat.BigTextStyle().bigText(body))
			.setNumber(push.unreadCount)
			.setCategory(NotificationCompat.CATEGORY_MESSAGE)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setAutoCancel(true)
			.setContentIntent(contentIntent)
			.build()
		NotificationManagerCompat.from(context).notify(id, notification)
	}

	/** Reads the push data out of the intent that opened the app, and remembers where it points. */
	fun consumeTap(extras: Bundle?) {
		extras ?: return
		val data = extras.keySet().associateWith { extras.getString(it) }
		targetFor(data)?.let { pending.value = it }
	}

	/**
	 * Decides where a tap on a push goes. A post is preferred: every push about a post carries its id. Comment and
	 * reply pushes open it with the comments shown. Without a post id it falls back to the person's profile.
	 */
	fun targetFor(data: Map<String, String?>): PushTarget? {
		// a chat message: Marpokat marks its pushes, and they carry the channel to open
		if (data[KEY_MARPOKAT] != null) {
			return data[KEY_CHAT_CHANNEL_ID]?.takeIf { it.isNotBlank() }?.let { PushTarget.Chat(it) }
		}
		val type = data[KEY_TYPE]
		val (idKey, usernameKey) = when {
			type == TYPE_NEW_POST -> KEY_AUTHOR_ID to KEY_AUTHOR_USERNAME
			type in ACTIVITY_TYPES -> KEY_ACTOR_ID to KEY_ACTOR_USERNAME
			else -> return null
		}
		data[KEY_POST_ID]?.takeIf { it.isNotBlank() }?.let { postId ->
			val isComment = type in COMMENT_TYPES
			return PushTarget.Post(
				postId = postId,
				openComments = isComment,
				commentId = data[KEY_COMMENT_ID]?.takeIf { isComment && it.isNotBlank() }
			)
		}
		val userId = data[idKey]?.takeIf { it.isNotBlank() } ?: return null
		val username = data[usernameKey]?.takeIf { it.isNotBlank() } ?: return null
		return PushTarget.Profile(userId, username)
	}
}
