package com.rizkysiregar.narrativ.ui.feature.onboarding

import androidx.lifecycle.ViewModel
import com.rizkysiregar.narrativ.data.cache.OnboardingPrefs
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.telemetry.AppAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
	private val onboardingPrefs: OnboardingPrefs,
	private val analytics: AppAnalytics
) : ViewModel() {

	fun markSeen() {
		onboardingPrefs.markSeen()
		analytics.log(AnalyticsEvents.ONBOARDING_COMPLETED)
	}
}
