package com.rizkysiregar.feedzmore.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Thin wrapper over Firebase Analytics so call sites stay free of the SDK and use the
 * event names in [AnalyticsEvents]. Never put personal data (email, caption, comment text) in a parameter.
 */
@Singleton
class AppAnalytics @Inject constructor(
	@ApplicationContext context: Context
) {
	private val analytics = FirebaseAnalytics.getInstance(context)

	fun log(event: String, vararg params: Pair<String, Any>) {
		val bundle = Bundle().apply {
			params.forEach { (key, value) ->
				when (value) {
					is String -> putString(key, value)
					is Int -> putLong(key, value.toLong())
					is Long -> putLong(key, value)
					is Boolean -> putString(key, value.toString())
					else -> putString(key, value.toString())
				}
			}
		}
		analytics.logEvent(event, bundle)
	}

	fun logScreen(route: String) {
		analytics.logEvent(
			FirebaseAnalytics.Event.SCREEN_VIEW,
			Bundle().apply { putString(FirebaseAnalytics.Param.SCREEN_NAME, route) }
		)
	}

	fun setCollectionEnabled(enabled: Boolean) {
		analytics.setAnalyticsCollectionEnabled(enabled)
	}

	/** Pass null on logout so the next person on this device is not linked to the previous one. */
	fun setUserId(userId: String?) {
		analytics.setUserId(userId)
	}
}
