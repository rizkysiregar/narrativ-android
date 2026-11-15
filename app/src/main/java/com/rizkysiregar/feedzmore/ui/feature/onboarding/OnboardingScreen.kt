package com.rizkysiregar.feedzmore.ui.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.ui.components.rememberReduceMotion
import com.rizkysiregar.feedzmore.ui.theme.Accent300
import com.rizkysiregar.feedzmore.ui.theme.Accent400
import com.rizkysiregar.feedzmore.ui.theme.Accent500
import com.rizkysiregar.feedzmore.ui.theme.Accent600
import com.rizkysiregar.feedzmore.ui.theme.Accent700
import com.rizkysiregar.feedzmore.ui.theme.BgBase
import com.rizkysiregar.feedzmore.ui.theme.BorderDefault
import com.rizkysiregar.feedzmore.ui.theme.BorderStrong
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme
import com.rizkysiregar.feedzmore.ui.theme.Like
import com.rizkysiregar.feedzmore.ui.theme.OnAccent
import com.rizkysiregar.feedzmore.ui.theme.RoseGold
import com.rizkysiregar.feedzmore.ui.theme.StoryRingBrush
import com.rizkysiregar.feedzmore.ui.theme.Success
import com.rizkysiregar.feedzmore.ui.theme.Surface2
import com.rizkysiregar.feedzmore.ui.theme.Surface3
import com.rizkysiregar.feedzmore.ui.theme.TextPrimary
import com.rizkysiregar.feedzmore.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.rizkysiregar.feedzmore.R
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes

private data class OnboardingPage(
	@StringRes val titleRes: Int,
	@StringRes val bodyRes: Int,
	val glow: Color
)

private val pages = listOf(
	OnboardingPage(
		titleRes = R.string.onb_title_1,
		bodyRes = R.string.onb_body_1,
		glow = Accent500
	),
	OnboardingPage(
		titleRes = R.string.onb_title_2,
		bodyRes = R.string.onb_body_2,
		glow = Like
	),
	OnboardingPage(
		titleRes = R.string.onb_title_3,
		bodyRes = R.string.onb_body_3,
		glow = RoseGold
	)
)

private val TextBlockHeight = 232.dp

@Composable
fun OnboardingScreen(
	notificationsEnabled: Boolean,
	onEnableNotifications: () -> Unit,
	onGetStarted: () -> Unit,
	onLogin: () -> Unit
) {
	val pagerState = rememberPagerState { pages.size }
	val scope = rememberCoroutineScope()
	val isLast = pagerState.currentPage == pages.lastIndex

	val glow by animateColorAsState(pages[pagerState.currentPage].glow, tween(600), label = "glow")
	val skipAlpha by animateFloatAsState(if (isLast) 0f else 1f, label = "skip")

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(BgBase)
			.drawBehind {
				drawRect(
					Brush.radialGradient(
						colors = listOf(glow.copy(alpha = 0.28f), Color.Transparent),
						center = Offset(size.width / 2, size.height * 0.27f),
						radius = size.width * 0.95f
					)
				)
			}
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.windowInsetsPadding(WindowInsets.systemBars)
				.padding(horizontal = 24.dp)
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.height(56.dp),
				horizontalArrangement = Arrangement.End,
				verticalAlignment = Alignment.CenterVertically
			) {
				TextButton(
					onClick = onLogin,
					enabled = !isLast,
					modifier = Modifier.alpha(skipAlpha)
				) {
					Text(stringResource(R.string.skip), color = TextSecondary)
				}
			}

			HorizontalPager(
				state = pagerState,
				modifier = Modifier.weight(1f)
			) { index ->
				PageContent(
					index = index,
					pagerState = pagerState,
					notificationsEnabled = notificationsEnabled,
					onEnableNotifications = onEnableNotifications
				)
			}

			PageIndicator(current = pagerState.currentPage, count = pages.size)

			Spacer(modifier = Modifier.height(24.dp))

			Button(
				onClick = {
					if (isLast) {
						onGetStarted()
					} else {
						scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
					}
				},
				modifier = Modifier
					.fillMaxWidth()
					.height(52.dp),
				shape = RoundedCornerShape(16.dp),
				colors = ButtonDefaults.buttonColors(containerColor = Accent500, contentColor = OnAccent)
			) {
				Text(
					text = if (isLast) stringResource(R.string.get_started) else stringResource(R.string.next),
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold
				)
			}

			// Fixed height so the primary button does not move when the login link appears.
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(56.dp),
				contentAlignment = Alignment.Center
			) {
				if (isLast) {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(
							text = stringResource(R.string.already_account),
							style = MaterialTheme.typography.bodyMedium,
							color = TextSecondary
						)
						TextButton(onClick = onLogin) {
							Text(stringResource(R.string.log_in), color = Accent400, fontWeight = FontWeight.SemiBold)
						}
					}
				}
			}
		}
	}
}

@Composable
private fun PageContent(
	index: Int,
	pagerState: PagerState,
	notificationsEnabled: Boolean,
	onEnableNotifications: () -> Unit
) {
	val page = pages[index]
	// 0 when this page is centered, +/-1 when a full page away.
	val offset = (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction
	val visibility = 1f - abs(offset).coerceIn(0f, 1f)

	Column(
		modifier = Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		// The illustration travels faster than the page, which gives the swipe some depth.
		BoxWithConstraints(
			modifier = Modifier
				.weight(1f)
				.fillMaxWidth()
				.graphicsLayer {
					translationX = -offset * size.width * 0.35f
					alpha = visibility
				},
			contentAlignment = Alignment.Center
		) {
			val side = minOf(maxWidth * 0.85f, maxHeight)
			when (index) {
				0 -> CardStackIllustration(side)
				1 -> MosaicIllustration(side)
				else -> PeopleIllustration(side)
			}
		}

		Column(
			modifier = Modifier
				.fillMaxWidth()
				.height(TextBlockHeight)
				.graphicsLayer { alpha = visibility },
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Spacer(modifier = Modifier.height(24.dp))
			Text(
				text = stringResource(page.titleRes),
				style = MaterialTheme.typography.headlineMedium,
				fontWeight = FontWeight.Bold,
				color = TextPrimary,
				textAlign = TextAlign.Center
			)
			Spacer(modifier = Modifier.height(12.dp))
			Text(
				text = stringResource(page.bodyRes),
				style = MaterialTheme.typography.bodyLarge,
				color = TextSecondary,
				textAlign = TextAlign.Center
			)
			if (index == pages.lastIndex) {
				Spacer(modifier = Modifier.height(16.dp))
				NotificationAction(notificationsEnabled, onEnableNotifications)
			}
		}
	}
}

@Composable
private fun NotificationAction(enabled: Boolean, onClick: () -> Unit) {
	if (enabled) {
		Row(
			modifier = Modifier.height(48.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(Icons.Filled.Check, contentDescription = null, tint = Success, modifier = Modifier.size(18.dp))
			Spacer(modifier = Modifier.width(8.dp))
			Text(stringResource(R.string.notifications_on), style = MaterialTheme.typography.bodyMedium, color = Success)
		}
	} else {
		OutlinedButton(
			onClick = onClick,
			modifier = Modifier.height(48.dp),
			shape = RoundedCornerShape(24.dp),
			border = BorderStroke(1.dp, BorderStrong)
		) {
			Icon(
				Icons.Filled.Notifications,
				contentDescription = null,
				tint = Accent300,
				modifier = Modifier.size(18.dp)
			)
			Spacer(modifier = Modifier.width(8.dp))
			Text(stringResource(R.string.turn_on_notifications), color = TextPrimary)
		}
	}
}

@Composable
private fun PageIndicator(current: Int, count: Int) {
	val pageDescription = stringResource(R.string.cd_page_of, current + 1, count)
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.semantics { contentDescription = pageDescription },
		horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
	) {
		repeat(count) { i ->
			val selected = i == current
			val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot-width")
			val color by animateColorAsState(if (selected) Accent500 else BorderStrong, label = "dot-color")
			Box(
				modifier = Modifier
					.height(8.dp)
					.width(width)
					.clip(CircleShape)
					.background(color)
			)
		}
	}
}

// ---------- Illustrations ----------

/** Slow vertical bob, in [-amplitude, amplitude]. Stays still when system animations are off. */
@Composable
private fun rememberFloatOffset(amplitude: Dp, delayMs: Int = 0): Dp {
	val reduceMotion = rememberReduceMotion()
	val value by rememberInfiniteTransition(label = "float").animateFloat(
		initialValue = -1f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(2400, easing = EaseInOutSine),
			repeatMode = RepeatMode.Reverse,
			initialStartOffset = StartOffset(delayMs)
		),
		label = "float-value"
	)
	return if (reduceMotion) 0.dp else amplitude * value
}

@Composable
private fun CardStackIllustration(side: Dp) {
	val bob = rememberFloatOffset(amplitude = 6.dp)
	Box(
		modifier = Modifier
			.size(side)
			.offset(y = bob),
		contentAlignment = Alignment.Center
	) {
		PostCard(side, rotation = -9f, dx = -0.12f, tint = Surface2, front = false)
		PostCard(side, rotation = 8f, dx = 0.12f, tint = Surface3, front = false)
		PostCard(side, rotation = 0f, dx = 0f, tint = Surface2, front = true)
	}
}

@Composable
private fun PostCard(side: Dp, rotation: Float, dx: Float, tint: Color, front: Boolean) {
	val shape = RoundedCornerShape(24.dp)
	Column(
		modifier = Modifier
			.offset(x = side * dx)
			.graphicsLayer { rotationZ = rotation }
			.width(side * 0.56f)
			.height(side * 0.76f)
			.clip(shape)
			.background(tint)
			.border(1.dp, BorderDefault, shape)
			.padding(12.dp)
	) {
		if (front) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Box(
					modifier = Modifier
						.size(28.dp)
						.clip(CircleShape)
						.background(StoryRingBrush)
				)
				Spacer(modifier = Modifier.width(8.dp))
				Column {
					Box(Modifier.width(56.dp).height(8.dp).clip(CircleShape).background(BorderStrong))
					Spacer(modifier = Modifier.height(4.dp))
					Box(Modifier.width(36.dp).height(6.dp).clip(CircleShape).background(BorderDefault))
				}
			}
			Spacer(modifier = Modifier.height(10.dp))
		}
		Box(
			modifier = Modifier
				.weight(1f)
				.fillMaxWidth()
				.clip(RoundedCornerShape(16.dp))
				.background(Brush.linearGradient(listOf(Accent700, Accent500, Like.copy(alpha = 0.8f)))),
			contentAlignment = Alignment.Center
		) {
			if (front) {
				Icon(
					Icons.Filled.PlayArrow,
					contentDescription = null,
					tint = Color.White.copy(alpha = 0.85f),
					modifier = Modifier.size(40.dp)
				)
			}
		}
		if (front) {
			Spacer(modifier = Modifier.height(10.dp))
			Row(verticalAlignment = Alignment.CenterVertically) {
				Icon(Icons.Filled.Favorite, contentDescription = null, tint = Like, modifier = Modifier.size(18.dp))
				Spacer(modifier = Modifier.width(6.dp))
				Box(Modifier.width(24.dp).height(6.dp).clip(CircleShape).background(BorderStrong))
				Spacer(modifier = Modifier.width(14.dp))
				Icon(Icons.Filled.ChatBubble, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
				Spacer(modifier = Modifier.width(6.dp))
				Box(Modifier.width(18.dp).height(6.dp).clip(CircleShape).background(BorderStrong))
			}
		}
	}
}

private val tileColors = listOf(
	listOf(Accent700, Accent500),
	listOf(Like, RoseGold),
	listOf(Surface3, Accent600),
	listOf(RoseGold, Accent700),
	listOf(Accent500, Like),
	listOf(Accent600, Surface3)
)

@Composable
private fun MosaicIllustration(side: Dp) {
	Column(
		modifier = Modifier.size(side),
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.height(40.dp)
				.clip(CircleShape)
				.background(Surface2)
				.border(1.dp, BorderDefault, CircleShape)
				.padding(horizontal = 14.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
			Spacer(modifier = Modifier.width(10.dp))
			Box(Modifier.width(88.dp).height(8.dp).clip(CircleShape).background(BorderStrong))
		}
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f),
			horizontalArrangement = Arrangement.spacedBy(10.dp)
		) {
			MosaicColumn(listOf(1.3f, 0.8f), firstTile = 0, modifier = Modifier.weight(1f))
			MosaicColumn(listOf(0.8f, 1.3f), firstTile = 2, modifier = Modifier.weight(1f))
			MosaicColumn(listOf(1.1f, 1f), firstTile = 4, modifier = Modifier.weight(1f))
		}
	}
}

@Composable
private fun MosaicColumn(weights: List<Float>, firstTile: Int, modifier: Modifier) {
	Column(
		modifier = modifier.fillMaxHeight(),
		verticalArrangement = Arrangement.spacedBy(10.dp)
	) {
		weights.forEachIndexed { i, weight ->
			val tile = firstTile + i
			PulsingTile(
				colors = tileColors[tile % tileColors.size],
				delayMs = tile * 220,
				modifier = Modifier
					.weight(weight)
					.fillMaxWidth()
			)
		}
	}
}

@Composable
private fun PulsingTile(colors: List<Color>, delayMs: Int, modifier: Modifier) {
	val reduceMotion = rememberReduceMotion()
	val pulse by rememberInfiniteTransition(label = "tile").animateFloat(
		initialValue = 0.7f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(1400, easing = EaseInOutSine),
			repeatMode = RepeatMode.Reverse,
			initialStartOffset = StartOffset(delayMs)
		),
		label = "tile-alpha"
	)
	Box(
		modifier = modifier
			.alpha(if (reduceMotion) 1f else pulse)
			.clip(RoundedCornerShape(16.dp))
			.background(Brush.linearGradient(colors))
	)
}

@Composable
private fun PeopleIllustration(side: Dp) {
	val bobA = rememberFloatOffset(amplitude = 6.dp)
	val bobB = rememberFloatOffset(amplitude = 6.dp, delayMs = 800)
	val bobC = rememberFloatOffset(amplitude = 5.dp, delayMs = 1600)

	Box(modifier = Modifier.size(side), contentAlignment = Alignment.Center) {
		Avatar(size = side * 0.22f, ringed = false, modifier = Modifier.offset(x = side * -0.32f, y = side * -0.26f + bobB))
		Avatar(size = side * 0.2f, ringed = false, modifier = Modifier.offset(x = side * 0.34f, y = side * 0.2f + bobC))

		Column(
			modifier = Modifier.offset(y = bobA),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Avatar(size = side * 0.46f, ringed = true)
			Spacer(modifier = Modifier.height(14.dp))
			Text(
				text = stringResource(R.string.follow),
				style = MaterialTheme.typography.labelLarge,
				fontWeight = FontWeight.SemiBold,
				color = OnAccent,
				modifier = Modifier
					.clip(CircleShape)
					.background(Accent500)
					.padding(horizontal = 18.dp, vertical = 8.dp)
			)
		}

		Chip(
			label = "2.4k",
			icon = Icons.Filled.Favorite,
			iconTint = Like,
			modifier = Modifier.offset(x = side * 0.27f, y = side * -0.3f + bobC)
		)
		Chip(
			label = stringResource(R.string.chip_love_this),
			icon = Icons.Filled.ChatBubble,
			iconTint = Accent400,
			modifier = Modifier.offset(x = side * -0.3f, y = side * 0.42f + bobB)
		)
	}
}

@Composable
private fun Avatar(size: Dp, ringed: Boolean, modifier: Modifier = Modifier) {
	val ringModifier = if (ringed) {
		Modifier.border(4.dp, StoryRingBrush, CircleShape).padding(8.dp)
	} else {
		Modifier.border(1.dp, BorderDefault, CircleShape)
	}
	Box(
		modifier = modifier
			.size(size)
			.then(ringModifier)
			.clip(CircleShape)
			.background(Surface3),
		contentAlignment = Alignment.Center
	) {
		Icon(Icons.Filled.Person, contentDescription = null, tint = Accent300, modifier = Modifier.fillMaxSize(0.6f))
	}
}

@Composable
private fun Chip(
	label: String,
	icon: androidx.compose.ui.graphics.vector.ImageVector,
	iconTint: Color,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier
			.clip(CircleShape)
			.background(Surface2)
			.border(1.dp, BorderDefault, CircleShape)
			.padding(horizontal = 12.dp, vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
		Spacer(modifier = Modifier.width(6.dp))
		Text(label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
	}
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0B14, heightDp = 800)
@Composable
fun OnboardingScreenPreview() {
	FeedzmoreTheme {
		OnboardingScreen(
			notificationsEnabled = false,
			onEnableNotifications = {},
			onGetStarted = {},
			onLogin = {}
		)
	}
}
