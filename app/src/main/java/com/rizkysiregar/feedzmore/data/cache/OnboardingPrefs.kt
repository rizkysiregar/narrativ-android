package com.rizkysiregar.feedzmore.data.cache

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers whether the intro was shown. It lives outside the DataStore on purpose:
 * [DataStoreManager.clearAll] wipes the session on logout and would bring the intro back.
 */
@Singleton
class OnboardingPrefs @Inject constructor(
	@ApplicationContext context: Context
) {
	private val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

	fun isSeen(): Boolean = prefs.getBoolean(KEY_SEEN, false)

	fun markSeen() {
		prefs.edit().putBoolean(KEY_SEEN, true).apply()
	}

	private companion object {
		const val KEY_SEEN = "seen"
	}
}
