package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizkysiregar.feedzmore.ui.components.rememberReduceMotion
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Software Engineer
 */

private const val BRAND = "feedzmore"
private val LetterLift = 6.dp
private const val WaveDurationMs = 900
private const val WaveLetterDelay = 0.7f

/**
 * Top bar of the feed: the brand label centered, search at the end.
 *
 * The letters react to the pull-to-refresh gesture: [pullProgress] sends a ripple through them
 * while the list is pulled, and [isRefreshing] keeps them waving until the refresh is done.
 */
@Composable
fun FeedHeader(
	pullProgress: Float,
	isRefreshing: Boolean,
	onSearchClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier
			.fillMaxWidth()
			.height(56.dp)
			.padding(horizontal = 4.dp)
	) {
		AnimatedBrandLabel(
			pullProgress = pullProgress,
			isRefreshing = isRefreshing,
			modifier = Modifier.align(Alignment.Center)
		)
		IconButton(
			onClick = onSearchClick,
			modifier = Modifier.align(Alignment.CenterEnd)
		) {
			Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
		}
	}
}

@Composable
private fun AnimatedBrandLabel(
	pullProgress: Float,
	isRefreshing: Boolean,
	modifier: Modifier = Modifier
) {
	val reduceMotion = rememberReduceMotion()
	val wave = remember { Animatable(0f) }

	LaunchedEffect(isRefreshing, reduceMotion) {
		wave.snapTo(0f)
		if (isRefreshing && !reduceMotion) {
			while (true) {
				wave.animateTo(2f * PI.toFloat(), tween(WaveDurationMs, easing = LinearEasing))
				wave.snapTo(0f)
			}
		}
	}

	Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
		BRAND.forEachIndexed { index, letter ->
			Text(
				text = letter.toString(),
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
				fontSize = 22.sp,
				modifier = Modifier.graphicsLayer {
					val lift = if (isRefreshing) {
						// a crest travelling along the word
						max(0f, sin(wave.value - index * WaveLetterDelay))
					} else {
						// letter `index` rises and settles as the pull passes its share of the distance
						val share = (pullProgress.coerceIn(0f, 1f) * BRAND.length - index).coerceIn(0f, 1f)
						sin(PI.toFloat() * share)
					}
					translationY = -LetterLift.toPx() * lift
					val scale = 1f + 0.12f * lift
					scaleX = scale
					scaleY = scale
				}
			)
		}
	}
}

/** Threads-style entry point: tapping the pill opens the post composer. */
@Composable
fun FeedComposerRow(
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Surface(
		modifier = modifier
			.fillMaxWidth()
			.clickable(onClick = onClick),
		shape = RoundedCornerShape(24.dp),
		color = MaterialTheme.colorScheme.surface,
		border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
	) {
		Row(
			modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = "Apa yang kamu pikirkan?",
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.weight(1f)
			)
			Icon(
				imageVector = Icons.Default.Add,
				contentDescription = "Buat post",
				tint = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}
