package com.rizkysiregar.feedzmore.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.BuildConfig
import com.rizkysiregar.feedzmore.config.RemoteConfigManager
import com.rizkysiregar.feedzmore.data.remote.ApiErrorEvent
import com.rizkysiregar.feedzmore.data.remote.ApiErrorManager
import com.rizkysiregar.feedzmore.data.session.SessionManager
import com.rizkysiregar.feedzmore.telemetry.AnalyticsEvents
import com.rizkysiregar.feedzmore.telemetry.AppAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
	sessionManager: SessionManager,
	apiErrorManager: ApiErrorManager,
	private val analytics: AppAnalytics,
	remoteConfigManager: RemoteConfigManager
) : ViewModel() {
	val sessionExpiredMessage: StateFlow<String?> = sessionManager.sessionExpiredMessage
	val apiErrors: SharedFlow<ApiErrorEvent> = apiErrorManager.errors

	/** The message of the blocking update dialog, or null while this version may keep running. */
	val forceUpdateMessage: StateFlow<String?> = remoteConfigManager.config
		.map { if (it.requiresUpdate(BuildConfig.VERSION_CODE.toLong())) it.updateMessage else null }
		.stateIn(viewModelScope, SharingStarted.Eagerly, null)

	fun onForceUpdateShown() = analytics.log(AnalyticsEvents.FORCE_UPDATE_SHOWN)

	/** Called on every navigation; [route] is the route pattern, so it carries no ids. */
	fun onScreenViewed(route: String) = analytics.logScreen(route)
}
