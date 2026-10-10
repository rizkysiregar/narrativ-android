package com.rizkysiregar.feedzmore.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Transcodes a selected video into an H.264/AAC MP4 that fits within
 * the backend size cap, targeting a bitrate derived from the clip duration.
 */
@Singleton
class VideoCompressor @Inject constructor(
	@ApplicationContext private val context: Context
) {

	suspend fun compress(inputUri: Uri): File {
		val durationMs = readDurationMs(inputUri)
			?: throw MediaValidationException("Durasi video tidak dapat dibaca")
		if (durationMs > MAX_DURATION_MS) {
			throw MediaValidationException("Durasi video maksimal 60 detik")
		}

		val durationSeconds = (durationMs / 1000.0).coerceAtLeast(1.0)
		val targetBitrate = ((TARGET_MAX_BYTES * 8) / durationSeconds)
			.toInt()
			.coerceIn(MIN_VIDEO_BITRATE, MAX_VIDEO_BITRATE)

		val outputFile = File(context.cacheDir, "feed_upload_${System.currentTimeMillis()}.mp4")
		if (outputFile.exists()) outputFile.delete()

		return withContext(Dispatchers.Main) {
			suspendCancellableCoroutine { continuation ->
				val encoderFactory = DefaultEncoderFactory.Builder(context)
					.setRequestedVideoEncoderSettings(
						VideoEncoderSettings.Builder()
							.setBitrate(targetBitrate)
							.build()
					)
					.setRequestedAudioEncoderSettings(
						AudioEncoderSettings.Builder()
							.setBitrate(AUDIO_BITRATE)
							.build()
					)
					.build()

				val transformer = Transformer.Builder(context)
					.setVideoMimeType(MimeTypes.VIDEO_H264)
					.setAudioMimeType(MimeTypes.AUDIO_AAC)
					.setEncoderFactory(encoderFactory)
					.addListener(object : Transformer.Listener {
						override fun onCompleted(composition: Composition, exportResult: ExportResult) {
							if (continuation.isActive) continuation.resume(outputFile)
						}

						override fun onError(
							composition: Composition,
							exportResult: ExportResult,
							exportException: ExportException
						) {
							Log.e(TAG, "Video compression failed", exportException)
							if (continuation.isActive) {
								continuation.resumeWithException(
									MediaValidationException("Gagal memproses video")
								)
							}
						}
					})
					.build()

				continuation.invokeOnCancellation {
					runCatching { transformer.cancel() }
					outputFile.delete()
				}

				transformer.start(
					EditedMediaItem.Builder(MediaItem.fromUri(inputUri)).build(),
					outputFile.absolutePath
				)
			}
		}
	}

	private fun readDurationMs(uri: Uri): Long? = runCatching {
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

	private companion object {
		const val TAG = "VideoCompressor"
		const val MAX_DURATION_MS = 60_000L
		const val MAX_VIDEO_BITRATE = 8_000_000
		const val MIN_VIDEO_BITRATE = 500_000
		const val AUDIO_BITRATE = 128_000

		// Keep a safety margin below the backend's 50MB hard cap.
		const val TARGET_MAX_BYTES = 45L * 1024 * 1024
	}
}
