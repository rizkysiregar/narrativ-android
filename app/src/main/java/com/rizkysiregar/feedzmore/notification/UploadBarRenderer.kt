package com.rizkysiregar.feedzmore.notification

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import com.rizkysiregar.feedzmore.ui.theme.Accent400
import com.rizkysiregar.feedzmore.ui.theme.Accent500
import com.rizkysiregar.feedzmore.ui.theme.Accent700
import com.rizkysiregar.feedzmore.ui.theme.BorderDefault
import com.rizkysiregar.feedzmore.ui.theme.Like
import com.rizkysiregar.feedzmore.ui.theme.RoseGold
import com.rizkysiregar.feedzmore.ui.theme.TextPrimary

/**
 * Draws the upload bar of the notification card: a track, a gradient fill and the paper plane
 * riding its tip. RemoteViews cannot draw custom shapes, so the bar is rendered to a bitmap.
 */
internal object UploadBarRenderer {

	const val WIDTH_PX = 640
	const val HEIGHT_COLLAPSED_PX = 40
	const val HEIGHT_EXPANDED_PX = 56

	fun render(percent: Int, heightPx: Int): Bitmap {
		val width = WIDTH_PX.toFloat()
		val height = heightPx.toFloat()
		val bitmap = Bitmap.createBitmap(WIDTH_PX, heightPx, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(bitmap)

		val planeSize = height * 0.4f
		val trackHeight = height * 0.18f
		val centerY = height / 2f
		val radius = trackHeight / 2f

		val paint = Paint(Paint.ANTI_ALIAS_FLAG)
		paint.color = BorderDefault.toArgb()
		canvas.drawRoundRect(RectF(0f, centerY - radius, width, centerY + radius), radius, radius, paint)

		// The plane's tail reaches 0.8 sizes behind the tip and its nose one size ahead, so keep both on the bitmap.
		val minTip = planeSize * 0.8f
		val maxTip = width - planeSize
		val tip = minTip + (maxTip - minTip) * (percent.coerceIn(0, 100) / 100f)

		paint.shader = LinearGradient(
			0f, 0f, width, 0f,
			intArrayOf(Accent500.toArgb(), Like.toArgb(), RoseGold.toArgb()),
			null,
			Shader.TileMode.CLAMP
		)
		canvas.drawRoundRect(RectF(0f, centerY - radius, tip, centerY + radius), radius, radius, paint)
		paint.shader = null

		canvas.save()
		canvas.translate(tip, centerY)
		canvas.rotate(-8f)
		drawPlane(canvas, paint, planeSize)
		canvas.restore()

		return bitmap
	}

	/** Same three folds as the in-app plane, pointing along +x with the nose at (size, 0). */
	private fun drawPlane(canvas: Canvas, paint: Paint, size: Float) {
		fun triangle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, color: Int) {
			val path = Path().apply {
				moveTo(ax, ay)
				lineTo(bx, by)
				lineTo(cx, cy)
				close()
			}
			paint.color = color
			canvas.drawPath(path, paint)
		}

		triangle(size, 0f, -size * 0.8f, -size * 0.5f, -size * 0.3f, 0f, TextPrimary.toArgb())
		triangle(size, 0f, -size * 0.3f, 0f, -size * 0.55f, size * 0.62f, Accent400.toArgb())
		triangle(-size * 0.3f, 0f, -size * 0.55f, size * 0.62f, -size * 0.8f, size * 0.3f, Accent700.toArgb())
	}
}
