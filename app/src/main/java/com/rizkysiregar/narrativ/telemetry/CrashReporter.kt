package com.rizkysiregar.narrativ.telemetry

/**
 * Reports handled failures as non-fatal issues, with breadcrumbs that explain what the app was doing.
 * Uncaught crashes need no call here: Crashlytics catches them by itself.
 */
interface CrashReporter {
	/** [context] names the flow (e.g. "post_upload") so non-fatals group and filter well. */
	fun recordNonFatal(throwable: Throwable, context: String, vararg keys: Pair<String, String>)

	fun breadcrumb(message: String)

	fun setCollectionEnabled(enabled: Boolean)

	/** Only the opaque user id, never the username or email. Pass null on logout. */
	fun setUserId(userId: String?)
}
