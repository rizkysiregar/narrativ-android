package com.rizkysiregar.feedzmore.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/11/26
 * Description: Shrinks a picked image to fit the backend size cap while keeping it
 * visually sharp: downscales to a feed-friendly resolution, applies EXIF rotation,
 * then re-encodes with the highest quality that stays under the target size.
 * Images that are already small enough are returned untouched.
 */
@Singleton
class ImageCompressor @Inject constructor(
	@ApplicationContext private val context: Context
) {

	/**
	 * Returns the file to upload and its content type. [source] is a private cache
	 * copy of the picked image; it is deleted when a new file is produced.
	 */
	suspend fun compress(source: File, mimeType: String): CompressedImage = withContext(Dispatchers.Default) {
		val bounds = readBounds(source)
		val longestSide = max(bounds.outWidth, bounds.outHeight)
		if (longestSide <= 0) throw MediaValidationException("Gambar tidak dapat dibaca")

		if (source.length() <= SKIP_BYTES && longestSide <= MAX_DIMENSION) {
			return@withContext CompressedImage(source, mimeType)
		}

		var bitmap = decodeScaled(source, bounds, MAX_DIMENSION)
			?: throw MediaValidationException("Gambar tidak dapat dibaca")
		bitmap = applyOrientation(bitmap, readOrientation(source))

		val hasAlpha = bitmap.hasAlpha() && mimeType != JPEG
		val format = if (hasAlpha || mimeType == WEBP) WEBP_FORMAT else Bitmap.CompressFormat.JPEG
		val outMime = if (format == Bitmap.CompressFormat.JPEG) JPEG else WEBP
		val output = File(context.cacheDir, "feed_upload_${UUID.randomUUID()}.${if (outMime == JPEG) "jpg" else "webp"}")

		try {
			repeat(MAX_SCALE_STEPS) {
				for (quality in QUALITY_STEPS) {
					output.outputStream().use { bitmap.compress(format, quality, it) }
					if (output.length() <= TARGET_BYTES) {
						source.delete()
						return@withContext CompressedImage(output, outMime)
					}
				}
				val scaled = Bitmap.createScaledBitmap(
					bitmap,
					(bitmap.width * SCALE_STEP).toInt().coerceAtLeast(1),
					(bitmap.height * SCALE_STEP).toInt().coerceAtLeast(1),
					true
				)
				if (scaled !== bitmap) bitmap.recycle()
				bitmap = scaled
			}
		} finally {
			bitmap.recycle()
		}

		output.delete()
		source.delete()
		throw MediaValidationException("Gambar terlalu besar dan tidak dapat dikompres, coba gambar lain")
	}

	private fun readBounds(file: File): BitmapFactory.Options =
		BitmapFactory.Options().apply {
			inJustDecodeBounds = true
			BitmapFactory.decodeFile(file.absolutePath, this)
		}

	/** Decodes with a power-of-two sample size, then scales the rest of the way. */
	private fun decodeScaled(file: File, bounds: BitmapFactory.Options, maxDimension: Int): Bitmap? {
		var sample = 1
		val longest = max(bounds.outWidth, bounds.outHeight)
		while (longest / (sample * 2) >= maxDimension) sample *= 2

		val decoded = BitmapFactory.decodeFile(
			file.absolutePath,
			BitmapFactory.Options().apply { inSampleSize = sample }
		) ?: return null

		val decodedLongest = max(decoded.width, decoded.height)
		if (decodedLongest <= maxDimension) return decoded

		val ratio = maxDimension.toFloat() / decodedLongest
		val scaled = Bitmap.createScaledBitmap(
			decoded,
			(decoded.width * ratio).toInt().coerceAtLeast(1),
			(decoded.height * ratio).toInt().coerceAtLeast(1),
			true
		)
		if (scaled !== decoded) decoded.recycle()
		return scaled
	}

	private fun readOrientation(file: File): Int = try {
		ExifInterface(file.absolutePath)
			.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
	} catch (e: Exception) {
		ExifInterface.ORIENTATION_NORMAL
	}

	private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
		val matrix = Matrix()
		when (orientation) {
			ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
			ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
			ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
			ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
			ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
			ExifInterface.ORIENTATION_TRANSPOSE -> {
				matrix.postRotate(90f)
				matrix.postScale(-1f, 1f)
			}
			ExifInterface.ORIENTATION_TRANSVERSE -> {
				matrix.postRotate(270f)
				matrix.postScale(-1f, 1f)
			}
			else -> return bitmap
		}
		val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
		if (rotated !== bitmap) bitmap.recycle()
		return rotated
	}

	private companion object {
		const val JPEG = "image/jpeg"
		const val WEBP = "image/webp"

		const val MAX_DIMENSION = 2048
		const val SKIP_BYTES = 4L * 1024 * 1024 // already fine, avoid a lossy re-encode
		const val TARGET_BYTES = (4.5 * 1024 * 1024).toLong() // margin under the 5MB cap
		const val SCALE_STEP = 0.85f
		const val MAX_SCALE_STEPS = 4
		val QUALITY_STEPS = intArrayOf(90, 85, 80, 75, 70)

		@Suppress("DEPRECATION")
		val WEBP_FORMAT: Bitmap.CompressFormat =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				Bitmap.CompressFormat.WEBP_LOSSY
			} else {
				Bitmap.CompressFormat.WEBP
			}
	}
}

/** Image ready to upload, with the content type matching its encoding. */
data class CompressedImage(
	val file: File,
	val contentType: String
)
