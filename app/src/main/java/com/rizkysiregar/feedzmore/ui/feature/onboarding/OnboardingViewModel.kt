package com.rizkysiregar.feedzmore.ui.feature.onboarding

import androidx.lifecycle.ViewModel
import com.rizkysiregar.feedzmore.data.cache.OnboardingPrefs
import com.rizkysiregar.feedzmore.telemetry.AnalyticsEvents
import com.rizkysiregar.feedzmore.telemetry.AppAnalytics
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
