package com.rizkysiregar.narrativ.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.BuildConfig
import com.rizkysiregar.narrativ.config.RemoteConfigManager
import com.rizkysiregar.narrativ.data.remote.ApiErrorEvent
import com.rizkysiregar.narrativ.data.remote.ApiErrorManager
import com.rizkysiregar.narrativ.data.session.SessionManager
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.telemetry.AppAnalytics
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
