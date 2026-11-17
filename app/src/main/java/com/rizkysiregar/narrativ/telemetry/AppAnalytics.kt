package com.rizkysiregar.narrativ.telemetry

/**
 * Usage analytics, behind an interface so call sites stay free of the SDK and use the event names in
 * [AnalyticsEvents]. Never put personal data (email, caption, comment text) in a parameter.
 */
interface AppAnalytics {
	fun log(event: String, vararg params: Pair<String, Any>)

	fun logScreen(route: String)

	fun setCollectionEnabled(enabled: Boolean)

	/** Pass null on logout so the next person on this device is not linked to the previous one. */
	fun setUserId(userId: String?)
}
