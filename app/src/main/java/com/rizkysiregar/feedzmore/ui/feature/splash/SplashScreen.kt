package com.rizkysiregar.feedzmore.ui.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.R
import com.rizkysiregar.feedzmore.ui.components.rememberReduceMotion
import com.rizkysiregar.feedzmore.ui.theme.Accent500
import com.rizkysiregar.feedzmore.ui.theme.BgBase
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme
import com.rizkysiregar.feedzmore.ui.theme.Like
import com.rizkysiregar.feedzmore.ui.theme.RoseGold
import com.rizkysiregar.feedzmore.ui.theme.TextMuted
import com.rizkysiregar.feedzmore.ui.theme.TextPrimary
import androidx.compose.ui.res.stringResource

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */


@Composable
fun SplashScreen() {
	val reduceMotion = rememberReduceMotion()

	// Staggered entrance: the story ring draws, the logo settles, then the text rises in.
	val ring = remember { Animatable(if (reduceMotion) 1f else 0f) }
	val logo = remember { Animatable(if (reduceMotion) 1f else 0f) }
	val text = remember { Animatable(if (reduceMotion) 1f else 0f) }
	LaunchedEffect(Unit) {
		if (reduceMotion) return@LaunchedEffect
		ring.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
		logo.animateTo(1f, tween(400))
		text.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
	}

	val glow by rememberInfiniteTransition(label = "splash-glow").animateFloat(
		initialValue = 0.16f,
		targetValue = if (reduceMotion) 0.16f else 0.30f,
		animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
		label = "glow"
	)

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(BgBase)
			.drawBehind {
				drawRect(
					Brush.radialGradient(
						colors = listOf(Accent500.copy(alpha = glow), Color.Transparent),
						center = Offset(size.width / 2, size.height / 2),
						radius = size.width * 0.8f
					)
				)
			},
		contentAlignment = Alignment.Center
	) {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Box(modifier = Modifier.size(132.dp), contentAlignment = Alignment.Center) {
				Canvas(modifier = Modifier.fillMaxSize()) {
					val stroke = 4.dp.toPx()
					rotate(-90f) {
						drawArc(
							brush = Brush.sweepGradient(listOf(Accent500, Like, RoseGold, Accent500)),
							startAngle = 0f,
							sweepAngle = 360f * ring.value,
							useCenter = false,
							topLeft = Offset(stroke / 2, stroke / 2),
							size = Size(size.width - stroke, size.height - stroke),
							style = Stroke(width = stroke, cap = StrokeCap.Round)
						)
					}
				}
				Image(
					painter = painterResource(R.drawable.ic_splash_logo),
					contentDescription = null,
					modifier = Modifier
						.size(120.dp)
						.graphicsLayer {
							alpha = logo.value
							scaleX = 0.8f + 0.2f * logo.value
							scaleY = 0.8f + 0.2f * logo.value
						}
				)
			}

			Spacer(modifier = Modifier.height(24.dp))

			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				modifier = Modifier.graphicsLayer {
					alpha = text.value
					translationY = (1f - text.value) * 12.dp.toPx()
				}
			) {
				Text(
					text = "Feedzmore",
					style = MaterialTheme.typography.headlineMedium,
					fontWeight = FontWeight.Bold,
					color = TextPrimary
				)
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = stringResource(R.string.splash_tagline),
					style = MaterialTheme.typography.bodyMedium,
					color = TextMuted
				)
			}
		}
	}

}


@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
	FeedzmoreTheme {
		SplashScreen()
	}
}
