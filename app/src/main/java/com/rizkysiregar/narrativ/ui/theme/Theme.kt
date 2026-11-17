package com.rizkysiregar.narrativ.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Obsidian Amethyst is a dark-only theme.
private val AmethystColorScheme = darkColorScheme(
	primary = Accent500,
	onPrimary = OnAccent,
	primaryContainer = AccentSubtle,
	onPrimaryContainer = Accent300,
	secondary = Accent300,
	onSecondary = OnAccent,
	secondaryContainer = Surface3,
	onSecondaryContainer = TextPrimary,
	tertiary = RoseGold,
	onTertiary = OnAccent,
	background = BgBase,
	onBackground = TextPrimary,
	surface = Surface1,
	onSurface = TextPrimary,
	surfaceVariant = Surface2,
	onSurfaceVariant = TextSecondary,
	surfaceTint = Accent500,
	surfaceContainerLowest = BgBase,
	surfaceContainerLow = Surface1,
	surfaceContainer = Surface2,
	surfaceContainerHigh = Surface3,
	surfaceContainerHighest = Surface3,
	outline = BorderDefault,
	outlineVariant = BorderSubtle,
	error = Error,
	onError = OnAccent,
	scrim = Scrim
)

@Composable
fun NarrativTheme(
	content: @Composable () -> Unit
) {
	MaterialTheme(
		colorScheme = AmethystColorScheme,
		typography = AppTypography,
		content = content
	)
}
