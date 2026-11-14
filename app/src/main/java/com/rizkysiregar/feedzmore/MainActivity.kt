package com.rizkysiregar.feedzmore

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rizkysiregar.feedzmore.ui.main.MainRoot

import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		// Swaps the launch theme for the app theme, so it must run before super.onCreate.
		installSplashScreen()
		super.onCreate(savedInstanceState)
		// The theme is dark-only, so keep the system bar icons light regardless of the device mode.
		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
			navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
		)
		setContent {
			FeedzmoreTheme {
				MainRoot()
			}
		}
	}
}

