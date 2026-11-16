package com.rizkysiregar.narrativ.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 11/11/26
 * Description: Helpers for the profile photo crop screen: decode the picked image upright for display,
 * then cut the chosen square out of it as a JPEG in cache.
 */
object AvatarCropper {

	private const val DISPLAY_MAX_DIMENSION = 2048
	private const val OUTPUT_SIZE = 768
	private const val OUTPUT_QUALITY = 90

	/** Decodes [uri] down to a displayable size with the EXIF rotation applied, or null if unreadable. */
	suspend fun load(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
		try {
			val resolver = context.contentResolver
			val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
			resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
			if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

			var sample = 1
			while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= DISPLAY_MAX_DIMENSION) sample *= 2
			val decoded = resolver.openInputStream(uri)?.use {
				BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
			} ?: return@withContext null

			val orientation = resolver.openInputStream(uri)?.use {
				ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
			} ?: ExifInterface.ORIENTATION_NORMAL

			val matrix = Matrix()
			when (orientation) {
				ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
				ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
				ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
				ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
				ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
				ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
				ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
				else -> return@withContext decoded
			}
			Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
				.also { if (it !== decoded) decoded.recycle() }
		} catch (e: Exception) {
			null
		} catch (e: OutOfMemoryError) {
			null
		}
	}

	/**
	 * Writes the square at [left],[top],[size] (bitmap pixels) to a JPEG in cache, scaled to at most
	 * 768 px. [filter] is baked in. Areas outside the bitmap are filled white. The caller deletes the file.
	 */
	suspend fun crop(
		context: Context,
		source: Bitmap,
		left: Float,
		top: Float,
		size: Float,
		filter: AvatarFilter = AvatarFilter.ORIGINAL
	): File =
		withContext(Dispatchers.Default) {
			val outSize = minOf(OUTPUT_SIZE, size.toInt().coerceAtLeast(1))
			val output = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
			val canvas = Canvas(output)
			canvas.drawColor(Color.WHITE)
			val src = Rect(
				left.toInt().coerceIn(0, source.width - 1),
				top.toInt().coerceIn(0, source.height - 1),
				(left + size).toInt().coerceIn(1, source.width),
				(top + size).toInt().coerceIn(1, source.height)
			)
			val paint = if (filter.isOriginal) null else Paint(Paint.FILTER_BITMAP_FLAG).apply {
				colorFilter = ColorMatrixColorFilter(ColorMatrix(filter.matrix))
			}
			canvas.drawBitmap(source, src, Rect(0, 0, outSize, outSize), paint)

			val file = File(context.cacheDir, "avatar_crop_${UUID.randomUUID()}.jpg")
			withContext(Dispatchers.IO) {
				file.outputStream().use { output.compress(Bitmap.CompressFormat.JPEG, OUTPUT_QUALITY, it) }
			}
			output.recycle()
			file
		}
}
