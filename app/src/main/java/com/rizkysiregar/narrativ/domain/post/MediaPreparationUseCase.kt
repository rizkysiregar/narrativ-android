package com.rizkysiregar.narrativ.domain.post

import android.content.Context
import android.net.Uri
import com.rizkysiregar.narrativ.media.ImageCompressor
import com.rizkysiregar.narrativ.media.MediaValidationException
import com.rizkysiregar.narrativ.media.VideoCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.rizkysiregar.narrativ.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Prepares the picked media into a stable local file before the
 * background upload runs: videos are transcoded, images are copied to cache.
 */
@Singleton
class MediaPreparationUseCase @Inject constructor(
	@ApplicationContext private val context: Context,
	private val videoCompressor: VideoCompressor,
	private val imageCompressor: ImageCompressor
) {

	private val _isPreparing = MutableStateFlow(false)
	val isPreparing: StateFlow<Boolean> = _isPreparing.asStateFlow()

	/**
	 * Copies/compresses the selected media into app cache and returns the local
	 * file plus the exact content type to request the presigned URL with.
	 * Throws [MediaValidationException] when the media cannot be accepted.
	 */
	suspend fun prepare(uri: Uri, mimeType: String?): PreparedMedia {
		_isPreparing.value = true
		try {
			return prepareInternal(uri, mimeType)
		} finally {
			_isPreparing.value = false
		}
	}

	/**
	 * Prepares every picked media in order, keeping [isPreparing] on for the whole batch. When one of them
	 * is rejected, the files already prepared are deleted so nothing is left behind in the cache.
	 */
	suspend fun prepareAll(items: List<PickedMedia>): List<PreparedMedia> {
		_isPreparing.value = true
		val prepared = mutableListOf<PreparedMedia>()
		try {
			items.forEach { prepared += prepareInternal(it.uri, it.mimeType) }
			return prepared
		} catch (e: Throwable) {
			withContext(Dispatchers.IO) { prepared.forEach { File(it.filePath).delete() } }
			throw e
		} finally {
			_isPreparing.value = false
		}
	}

	private suspend fun prepareInternal(uri: Uri, mimeType: String?): PreparedMedia {
		val resolvedMime = mimeType ?: context.contentResolver.getType(uri)
			?: throw MediaValidationException(R.string.media_err_unknown_type)

		return when {
			resolvedMime.startsWith("image/") -> {
				if (resolvedMime !in ALLOWED_IMAGE_TYPES) {
					throw MediaValidationException(R.string.media_err_image_format)
				}
				val file = withContext(Dispatchers.IO) {
					copyToCache(uri, mimeToExtension(resolvedMime))
				}
				// GIF is passed through as-is: re-encoding would drop the animation.
				if (resolvedMime == "image/gif") {
					if (file.length() > MAX_IMAGE_BYTES) {
						file.delete()
						throw MediaValidationException(R.string.media_err_gif_size)
					}
					return PreparedMedia(file.absolutePath, resolvedMime)
				}
				val image = imageCompressor.compress(file, resolvedMime)
				PreparedMedia(image.file.absolutePath, image.contentType)
			}

			resolvedMime in ALLOWED_VIDEO_TYPES -> {
				val file = videoCompressor.compress(uri)
				if (file.length() > MAX_VIDEO_BYTES) {
					file.delete()
					throw MediaValidationException(R.string.media_err_video_size)
				}
				PreparedMedia(file.absolutePath, VIDEO_MP4)
			}

			else -> throw MediaValidationException(
				R.string.media_err_format
			)
		}
	}

	private fun copyToCache(uri: Uri, extension: String): File {
		val file = File(context.cacheDir, "feed_upload_${UUID.randomUUID()}.$extension")
		context.contentResolver.openInputStream(uri)?.use { input ->
			file.outputStream().use { output -> input.copyTo(output) }
		} ?: throw MediaValidationException(R.string.media_err_read)
		return file
	}

	private fun mimeToExtension(mimeType: String): String = when (mimeType) {
		"image/png" -> "png"
		"image/webp" -> "webp"
		"image/gif" -> "gif"
		else -> "jpg"
	}

	private companion object {
		const val VIDEO_MP4 = "video/mp4"
		const val MAX_IMAGE_BYTES = 5L * 1024 * 1024
		const val MAX_VIDEO_BYTES = 50L * 1024 * 1024

		val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
		val ALLOWED_VIDEO_TYPES = setOf("video/mp4", "video/quicktime", "video/webm")
	}
}

/**
 * Local file ready to be uploaded, with the content type that must be used both
 * for the presigned URL request and the storage PUT (it is signed).
 */
data class PreparedMedia(
	val filePath: String,
	val contentType: String
)

/** A media the user picked, before it is compressed or copied. */
data class PickedMedia(
	val uri: Uri,
	val mimeType: String?
)
