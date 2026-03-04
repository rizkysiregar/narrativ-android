package com.rizkysiregar.feedzmore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rizkysiregar.feedzmore.ui.main.MainRoot

import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			FeedzmoreTheme {
				MainRoot()
			}
		}
	}
}

