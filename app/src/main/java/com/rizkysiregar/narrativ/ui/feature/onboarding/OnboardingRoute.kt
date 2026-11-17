package com.rizkysiregar.narrativ.ui.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rizkysiregar.narrativ.ui.main.navigation.routes.AuthPathRoutes
import com.rizkysiregar.narrativ.ui.main.navigation.routes.MainPathRoute

@Composable
fun OnboardingRoute(
	navController: NavController,
	vm: OnboardingViewModel = hiltViewModel()
) {
	val context = LocalContext.current

	var notificationsEnabled by remember {
		mutableStateOf(
			Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
				ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
				PackageManager.PERMISSION_GRANTED
		)
	}
	val notificationLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { granted -> notificationsEnabled = granted }

	fun finish(thenRegister: Boolean) {
		vm.markSeen()
		navController.navigate(AuthPathRoutes.Login.route) {
			popUpTo(MainPathRoute.Onboarding.route) { inclusive = true }
		}
		// Login stays underneath, so back from Register lands on Login.
		if (thenRegister) navController.navigate(AuthPathRoutes.Register.route)
	}

	OnboardingScreen(
		notificationsEnabled = notificationsEnabled,
		onEnableNotifications = {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
			}
		},
		onGetStarted = { finish(thenRegister = true) },
		onLogin = { finish(thenRegister = false) }
	)
}
