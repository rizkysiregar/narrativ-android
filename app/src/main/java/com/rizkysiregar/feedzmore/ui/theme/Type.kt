package com.rizkysiregar.feedzmore.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.rizkysiregar.feedzmore.R

val Inter = FontFamily(
	Font(R.font.inter_regular, FontWeight.Normal),
	Font(R.font.inter_medium, FontWeight.Medium),
	Font(R.font.inter_semibold, FontWeight.SemiBold),
	Font(R.font.inter_bold, FontWeight.Bold)
)

private val DefaultTypography = Typography()

/** Uses Inter for every Material role so individual components never fall back to Roboto. */
val AppTypography = Typography(
	displayLarge = DefaultTypography.displayLarge.copy(fontFamily = Inter),
	displayMedium = DefaultTypography.displayMedium.copy(fontFamily = Inter),
	displaySmall = DefaultTypography.displaySmall.copy(fontFamily = Inter),
	headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = Inter),
	headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = Inter),
	headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = Inter),
	titleLarge = DefaultTypography.titleLarge.copy(fontFamily = Inter),
	titleMedium = DefaultTypography.titleMedium.copy(fontFamily = Inter),
	titleSmall = DefaultTypography.titleSmall.copy(fontFamily = Inter),
	bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = Inter),
	bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = Inter),
	bodySmall = DefaultTypography.bodySmall.copy(fontFamily = Inter),
	labelLarge = DefaultTypography.labelLarge.copy(fontFamily = Inter),
	labelMedium = DefaultTypography.labelMedium.copy(fontFamily = Inter),
	labelSmall = DefaultTypography.labelSmall.copy(fontFamily = Inter)
)
