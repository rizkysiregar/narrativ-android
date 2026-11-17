package com.rizkysiregar.narrativ.telemetry

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/** [CrashReporter] on Firebase Crashlytics. */
@Singleton
class FirebaseCrashReporter @Inject constructor() : CrashReporter {
	private val crashlytics get() = FirebaseCrashlytics.getInstance()

	override fun recordNonFatal(throwable: Throwable, context: String, vararg keys: Pair<String, String>) {
		crashlytics.apply {
			setCustomKey("flow", context)
			keys.forEach { (key, value) -> setCustomKey(key, value) }
			recordException(throwable)
		}
	}

	override fun breadcrumb(message: String) {
		crashlytics.log(message)
	}

	override fun setCollectionEnabled(enabled: Boolean) {
		crashlytics.isCrashlyticsCollectionEnabled = enabled
	}

	override fun setUserId(userId: String?) {
		crashlytics.setUserId(userId.orEmpty())
	}
}
