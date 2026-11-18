package com.rizkysiregar.narrativ.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private const val SWEEP_MS = 1600

/** Phase value that means "hold still, no highlight". */
private const val STILL = -1f

private val SkeletonShape = RoundedCornerShape(8.dp)

/**
 * The position of the light that sweeps over the placeholder blocks of a screen, from 0 to 1, one cycle every
 * [SWEEP_MS] ms. Share one between all the blocks of a screen so they sweep together. It is read while drawing, so
 * it never recomposes anything. It holds still when the user turned animations off.
 */
@Composable
fun rememberSkeletonPhase(): State<Float> {
	if (rememberReduceMotion()) return remember { mutableFloatStateOf(STILL) }
	return rememberInfiniteTransition(label = "skeleton").animateFloat(
		initialValue = 0f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(tween(SWEEP_MS, easing = LinearEasing), RepeatMode.Restart),
		label = "skeleton-phase"
	)
}

/**
 * Draws this block as a placeholder: a faint shape of the theme's text color with a soft light passing over it. Kept
 * low in contrast on purpose, it should say "something is coming" without drawing the eye.
 */
@Composable
fun Modifier.skeleton(phase: State<Float>, shape: Shape = SkeletonShape): Modifier {
	val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
	val light = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f)
	return this
		.clip(shape)
		.drawBehind {
			drawRect(base)
			val p = phase.value
			if (p != STILL) {
				// the light is one block wide and travels from fully left of the block to fully right of it
				val startX = -size.width + p * 2f * size.width
				drawRect(
					Brush.horizontalGradient(
						colors = listOf(Color.Transparent, light, Color.Transparent),
						startX = startX,
						endX = startX + size.width
					)
				)
			}
		}
}
