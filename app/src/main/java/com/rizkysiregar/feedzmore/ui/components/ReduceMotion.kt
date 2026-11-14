package com.rizkysiregar.feedzmore.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** True when the user turned system animations off, so decorative loops should stay still. */
@Composable
fun rememberReduceMotion(): Boolean {
	val context = LocalContext.current
	return remember {
		Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
	}
}
