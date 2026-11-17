package com.rizkysiregar.narrativ.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** [AppAnalytics] on Firebase Analytics. */
@Singleton
class FirebaseAppAnalytics @Inject constructor(
	@ApplicationContext context: Context
) : AppAnalytics {
	private val analytics = FirebaseAnalytics.getInstance(context)

	override fun log(event: String, vararg params: Pair<String, Any>) {
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

	override fun logScreen(route: String) {
		analytics.logEvent(
			FirebaseAnalytics.Event.SCREEN_VIEW,
			Bundle().apply { putString(FirebaseAnalytics.Param.SCREEN_NAME, route) }
		)
	}

	override fun setCollectionEnabled(enabled: Boolean) {
		analytics.setAnalyticsCollectionEnabled(enabled)
	}

	override fun setUserId(userId: String?) {
		analytics.setUserId(userId)
	}
}
