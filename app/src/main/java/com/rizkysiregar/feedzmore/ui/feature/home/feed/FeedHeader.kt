package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizkysiregar.feedzmore.ui.components.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlin.math.PI
import kotlin.math.sin

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Software Engineer
 */

private const val BRAND = "feedzmore"
private const val PromptIntervalMs = 3500L
private const val PromptTransitionMs = 500
private val ComposerPrompts = listOf(
	"Apa yang kamu pikirkan?",
	"Bagikan momen hari ini…",
	"Ada cerita seru hari ini?",
	"Lagi di mana sekarang?",
	"Foto atau video apa yang mau kamu bagikan?",
	"Tulis sesuatu untuk komunitasmu…",
	"Apa yang bikin harimu berwarna?"
)
private val LetterLift = 6.dp
private const val WaveCycleMs = 1800
// per-letter start offset and the share of a cycle each letter spends moving; the last letter
// starts at 8 * 0.06 and ends at 0.48 + 0.5, inside the cycle
private const val WaveLetterDelay = 0.06f
private const val WaveLetterSpan = 0.5f

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
	val currentRefreshing by rememberUpdatedState(isRefreshing)
	val wave = remember { Animatable(0f) }
	var isWaving by remember { mutableStateOf(false) }

	// The wave stands in for the loading indicator, so once started it always plays out the
	// current cycle, even when the request finishes sooner, and only then settles.
	LaunchedEffect(reduceMotion) {
		if (reduceMotion) return@LaunchedEffect
		snapshotFlow { currentRefreshing }
			.filter { it }
			.collect {
				isWaving = true
				do {
					wave.snapTo(0f)
					wave.animateTo(1f, tween(WaveCycleMs, easing = LinearEasing))
				} while (currentRefreshing)
				isWaving = false
				wave.snapTo(0f)
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
					val lift = if (isWaving) {
						// one crest runs along the word per cycle and every letter ends at rest
						bump((wave.value - index * WaveLetterDelay) / WaveLetterSpan)
					} else {
						// letter `index` rises and settles as the pull passes its share of the distance
						bump(pullProgress.coerceIn(0f, 1f) * BRAND.length - index)
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

/** Smooth 0 -> 1 -> 0 hill over [0, 1] with zero slope at both ends, flat outside it. */
private fun bump(x: Float): Float {
	val s = sin(PI.toFloat() * x.coerceIn(0f, 1f))
	return s * s
}

/** Cycles through [ComposerPrompts]; each new line rises in while the old one lifts away. */
@Composable
private fun RotatingPlaceholder(modifier: Modifier = Modifier) {
	val reduceMotion = rememberReduceMotion()
	var index by remember { mutableIntStateOf(0) }

	LaunchedEffect(Unit) {
		while (true) {
			delay(PromptIntervalMs)
			index = (index + 1) % ComposerPrompts.size
		}
	}

	AnimatedContent(
		targetState = index,
		modifier = modifier.clipToBounds(),
		transitionSpec = {
			if (reduceMotion) {
				fadeIn(tween(PromptTransitionMs)) togetherWith fadeOut(tween(PromptTransitionMs))
			} else {
				(slideInVertically(tween(PromptTransitionMs, easing = FastOutSlowInEasing)) { it } +
					fadeIn(tween(PromptTransitionMs, delayMillis = PromptTransitionMs / 4))) togetherWith
					(slideOutVertically(tween(PromptTransitionMs, easing = FastOutSlowInEasing)) { -it } +
						fadeOut(tween(PromptTransitionMs / 2)))
			}
		},
		label = "composerPrompt"
	) { promptIndex ->
		Text(
			text = ComposerPrompts[promptIndex],
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
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
			RotatingPlaceholder(modifier = Modifier.weight(1f))
			Icon(
				imageVector = Icons.Default.Add,
				contentDescription = "Buat post",
				tint = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}
