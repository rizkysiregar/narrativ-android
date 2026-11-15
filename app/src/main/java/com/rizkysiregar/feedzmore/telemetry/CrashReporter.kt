package com.rizkysiregar.feedzmore.telemetry

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Reports handled failures to Crashlytics as non-fatal issues, with breadcrumbs that explain
 * what the app was doing. Uncaught crashes need no call here: Crashlytics catches them by itself.
 */
@Singleton
class CrashReporter @Inject constructor() {
	private val crashlytics get() = FirebaseCrashlytics.getInstance()

	/** [context] names the flow (e.g. "post_upload") so non-fatals group and filter well. */
	fun recordNonFatal(throwable: Throwable, context: String, vararg keys: Pair<String, String>) {
		crashlytics.apply {
			setCustomKey("flow", context)
			keys.forEach { (key, value) -> setCustomKey(key, value) }
			recordException(throwable)
		}
	}

	fun breadcrumb(message: String) {
		crashlytics.log(message)
	}

	fun setCollectionEnabled(enabled: Boolean) {
		crashlytics.isCrashlyticsCollectionEnabled = enabled
	}

	/** Only the opaque user id, never the username or email. Pass null on logout. */
	fun setUserId(userId: String?) {
		crashlytics.setUserId(userId.orEmpty())
	}
}
