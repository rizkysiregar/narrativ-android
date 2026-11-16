package com.rizkysiregar.narrativ

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rizkysiregar.narrativ.ui.main.MainRoot

import com.rizkysiregar.narrativ.ui.theme.NarrativTheme

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
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
			NarrativTheme {
				MainRoot()
			}
		}
	}
}

