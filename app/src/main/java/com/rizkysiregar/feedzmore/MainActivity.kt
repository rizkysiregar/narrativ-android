package com.rizkysiregar.feedzmore

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import com.rizkysiregar.feedzmore.ui.presentation.auth.AuthRoute
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreandroidnativeTheme

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			FeedzmoreandroidnativeTheme {
				AuthRoute()
			}
		}
	}
}

