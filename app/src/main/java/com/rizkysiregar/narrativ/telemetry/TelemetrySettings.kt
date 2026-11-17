package com.rizkysiregar.narrativ.telemetry

import android.content.Context
import com.rizkysiregar.narrativ.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: The user's choice to share usage data and crash reports, on by default and switched off in
 * Settings. It turns Analytics, Crashlytics and Performance Monitoring on or off together. It lives outside
 * the DataStore on purpose: [com.rizkysiregar.narrativ.data.cache.DataStoreManager.clearAll] wipes the
 * session on logout and would silently turn data sharing back on.
 */
@Singleton
class TelemetrySettings @Inject constructor(
	@ApplicationContext context: Context,
	private val analytics: AppAnalytics,
	private val crashReporter: CrashReporter,
	private val perfTracer: PerfTracer
) {
	private val prefs = context.getSharedPreferences("telemetry", Context.MODE_PRIVATE)

	private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
	val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

	/** Applies the saved choice to the SDKs; called on every app start. */
	fun apply() = applyToSdks(_enabled.value)

	fun setEnabled(enabled: Boolean) {
		prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
		_enabled.value = enabled
		applyToSdks(enabled)
	}

	// Debug builds never report, whatever the user chose: an explicit "true" here would override the
	// collection flags of the debug manifest.
	private fun applyToSdks(userEnabled: Boolean) {
		val collect = userEnabled && !BuildConfig.DEBUG
		analytics.setCollectionEnabled(collect)
		crashReporter.setCollectionEnabled(collect)
		perfTracer.setCollectionEnabled(collect)
	}

	private companion object {
		const val KEY_ENABLED = "enabled"
	}
}
