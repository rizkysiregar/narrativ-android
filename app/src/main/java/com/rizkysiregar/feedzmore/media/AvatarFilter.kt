package com.rizkysiregar.feedzmore.media

import android.graphics.ColorMatrix
import androidx.annotation.StringRes
import com.rizkysiregar.feedzmore.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 11/11/26
 * Description: Basic colour presets for profile photos. Each is a 4x5 colour matrix so the same
 * preset drives the live preview, the thumbnails and the exported crop.
 */
enum class AvatarFilter(@StringRes val labelRes: Int, val matrix: FloatArray) {
	ORIGINAL(R.string.filter_original, ColorMatrix().array),
	MONO(R.string.filter_mono, ColorMatrix().apply { setSaturation(0f) }.array),
	SEPIA(
		R.string.filter_sepia,
		floatArrayOf(
			0.393f, 0.769f, 0.189f, 0f, 0f,
			0.349f, 0.686f, 0.168f, 0f, 0f,
			0.272f, 0.534f, 0.131f, 0f, 0f,
			0f, 0f, 0f, 1f, 0f
		)
	),
	VIVID(R.string.filter_vivid, build(saturation = 1.4f, contrast = 1.1f, brightness = 8f)),
	WARM(R.string.filter_warm, build(saturation = 1.1f, red = 1.1f, blue = 0.9f)),
	COOL(R.string.filter_cool, build(saturation = 1.05f, red = 0.9f, blue = 1.12f)),
	FADE(R.string.filter_fade, build(saturation = 0.75f, contrast = 0.85f, brightness = 24f)),
	DRAMATIC(R.string.filter_dramatic, build(saturation = 0.6f, contrast = 1.35f));

	val isOriginal: Boolean get() = this == ORIGINAL
}

private fun build(
	saturation: Float = 1f,
	contrast: Float = 1f,
	brightness: Float = 0f,
	red: Float = 1f,
	blue: Float = 1f
): FloatArray {
	val result = ColorMatrix().apply { setSaturation(saturation) }
	result.postConcat(ColorMatrix(floatArrayOf(
		red, 0f, 0f, 0f, 0f,
		0f, 1f, 0f, 0f, 0f,
		0f, 0f, blue, 0f, 0f,
		0f, 0f, 0f, 1f, 0f
	)))
	val offset = (1f - contrast) * 127.5f + brightness
	result.postConcat(ColorMatrix(floatArrayOf(
		contrast, 0f, 0f, 0f, offset,
		0f, contrast, 0f, 0f, offset,
		0f, 0f, contrast, 0f, offset,
		0f, 0f, 0f, 1f, 0f
	)))
	return result.array
}
