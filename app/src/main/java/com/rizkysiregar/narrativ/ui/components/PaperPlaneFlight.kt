package com.rizkysiregar.narrativ.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.rizkysiregar.narrativ.ui.theme.Accent300
import com.rizkysiregar.narrativ.ui.theme.Accent400
import com.rizkysiregar.narrativ.ui.theme.Accent700
import com.rizkysiregar.narrativ.ui.theme.Like
import com.rizkysiregar.narrativ.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sin

private const val FLIGHT_MILLIS = 1200
private const val TRAIL_FRACTION = 0.35f

/**
 * A paper plane that takes off from [origin] (root coordinates), swoops up and leaves through the
 * top-right corner, trailing a dashed line. It only draws, so touches pass through to the screen
 * below, and [onFinished] fires once the plane is gone (immediately when animations are off).
 */
@Composable
fun PaperPlaneFlight(
	origin: Offset,
	onFinished: () -> Unit,
	modifier: Modifier = Modifier
) {
	val reduceMotion = rememberReduceMotion()
	val progress = remember { Animatable(0f) }
	var rootOffset by remember { mutableStateOf(Offset.Zero) }

	LaunchedEffect(Unit) {
		if (!reduceMotion) {
			progress.animateTo(1f, tween(FLIGHT_MILLIS, easing = CubicBezierEasing(0.35f, 0f, 0.8f, 0.65f)))
		}
		onFinished()
	}

	Box(
		modifier
			.fillMaxSize()
			.onGloballyPositioned { rootOffset = it.positionInRoot() }
			.drawWithCache {
				val start = origin - rootOffset
				val w = size.width
				val h = size.height
				// Up and back first, then a long sweep out of the top-right corner.
				val path = Path().apply {
					moveTo(start.x, start.y)
					cubicTo(
						start.x - w * 0.28f, start.y - h * 0.22f,
						w * 0.6f, -h * 0.05f,
						w * 1.25f, -h * 0.12f
					)
				}
				val measure = PathMeasure().apply { setPath(path, false) }
				val length = measure.length
				val planeSize = 26.dp.toPx()
				val trailStroke = Stroke(
					width = 3.dp.toPx(),
					cap = StrokeCap.Round,
					pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 14.dp.toPx()))
				)

				onDrawBehind {
					val p = progress.value
					val distance = length * p
					val position = measure.getPosition(distance)
					val tangent = measure.getTangent(distance)

					val trailStart = max(0f, distance - length * TRAIL_FRACTION)
					if (distance - trailStart > 4f) {
						val trail = Path()
						measure.getSegment(trailStart, distance, trail, true)
						drawPath(
							path = trail,
							brush = Brush.linearGradient(
								colors = listOf(Color.Transparent, Accent300.copy(alpha = 0.8f), Like),
								start = measure.getPosition(trailStart),
								end = position
							),
							style = trailStroke
						)
					}

					// Grows out of the collapsing card, then shrinks a little as it flies into the distance.
					val grow = lerp(0.35f, 1f, (p / 0.15f).coerceIn(0f, 1f))
					val shrink = lerp(1f, 0.75f, ((p - 0.7f) / 0.3f).coerceIn(0f, 1f))
					val angle = atan2(tangent.y, tangent.x) * 180f / PI.toFloat()
					val bank = sin(p * 16f) * 5f
					val alpha = (p / 0.08f).coerceIn(0f, 1f)

					withTransform({
						translate(position.x, position.y)
						rotate(angle + bank, Offset.Zero)
						scale(grow * shrink, grow * shrink, Offset.Zero)
					}) {
						drawPlane(planeSize, alpha)
					}
				}
			}
	)
}

/** The plane points along +x, with its nose at (size, 0). */
private fun DrawScope.drawPlane(size: Float, alpha: Float) {
	val nose = Offset(size, 0f)
	val tailTop = Offset(-size * 0.8f, -size * 0.5f)
	val notch = Offset(-size * 0.3f, 0f)
	val tailBottom = Offset(-size * 0.8f, size * 0.3f)
	val keel = Offset(-size * 0.55f, size * 0.62f)

	fun triangle(a: Offset, b: Offset, c: Offset, color: Color) {
		val path = Path().apply {
			moveTo(a.x, a.y)
			lineTo(b.x, b.y)
			lineTo(c.x, c.y)
			close()
		}
		drawPath(path, color.copy(alpha = alpha))
	}

	triangle(nose, tailTop, notch, TextPrimary)
	triangle(nose, notch, keel, Accent400)
	triangle(notch, keel, tailBottom, Accent700)
}
