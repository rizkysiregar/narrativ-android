package com.rizkysiregar.narrativ.domain.post

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.StringRes
import com.rizkysiregar.narrativ.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** What a post accepts. The backend enforces the same limits, so they are checked here before anything is uploaded. */
object MediaLimits {
	val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
	val ALLOWED_VIDEO_TYPES = setOf("video/mp4", "video/quicktime", "video/webm")
	const val MAX_GIF_BYTES = 5L * 1024 * 1024
	const val MAX_VIDEO_BYTES = 50L * 1024 * 1024
	const val MAX_VIDEO_DURATION_MS = 60_000L
}

/** What the app can read about a picked media; the part of validation that needs the system. */
interface MediaInspector {
	fun mimeType(uri: Uri): String?

	/** null when the size cannot be read. */
	fun sizeBytes(uri: Uri): Long?

	/** null when the duration cannot be read. */
	fun durationMs(uri: Uri): Long?
}

class AndroidMediaInspector @Inject constructor(
	@ApplicationContext private val context: Context
) : MediaInspector {

	override fun mimeType(uri: Uri): String? = context.contentResolver.getType(uri)

	override fun sizeBytes(uri: Uri): Long? = runCatching {
		context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
	}.getOrNull()

	override fun durationMs(uri: Uri): Long? = runCatching {
		val retriever = MediaMetadataRetriever()
		try {
			context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
				retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
			}
			retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
		} finally {
			retriever.release()
		}
	}.getOrNull()
}

/** The verdict on one picked media: its type, and the string resource of the reason to refuse it (null = accepted). */
data class MediaCheck(val mimeType: String?, @StringRes val errorRes: Int?)

class MediaValidator @Inject constructor(
	private val inspector: MediaInspector
) {

	fun check(uri: Uri): MediaCheck {
		val mimeType = inspector.mimeType(uri)
		return MediaCheck(mimeType, errorFor(uri, mimeType))
	}

	@StringRes
	private fun errorFor(uri: Uri, mimeType: String?): Int? {
		if (mimeType == null) return R.string.media_err_unknown_type
		return when {
			mimeType.startsWith("image/") -> when {
				mimeType !in MediaLimits.ALLOWED_IMAGE_TYPES -> R.string.media_err_image_format
				// Static images are compressed before upload; GIFs are sent as-is.
				mimeType == "image/gif" && (inspector.sizeBytes(uri) ?: 0L) > MediaLimits.MAX_GIF_BYTES -> R.string.media_err_gif_size
				else -> null
			}

			mimeType in MediaLimits.ALLOWED_VIDEO_TYPES -> when {
				(inspector.sizeBytes(uri) ?: 0L) > MediaLimits.MAX_VIDEO_BYTES -> R.string.media_err_video_50
				(inspector.durationMs(uri) ?: 0L) > MediaLimits.MAX_VIDEO_DURATION_MS -> R.string.media_err_duration_max
				else -> null
			}

			else -> R.string.media_err_format
		}
	}
}
