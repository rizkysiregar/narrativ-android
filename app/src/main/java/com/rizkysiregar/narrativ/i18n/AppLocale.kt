package com.rizkysiregar.narrativ.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/** The languages the app is translated into. English is also the fallback for every other device language. */
enum class AppLanguage(val tag: String) {
	ENGLISH("en"),
	INDONESIAN("id");

	companion object {
		fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }
	}
}

/**
 * The language of the app: the one picked in Settings (or in the system's per-app language screen on Android 13+),
 * else the device language when it is Indonesian, else English. Switching recreates the running activities.
 */
object AppLocale {

	/** The explicit choice, or null while the app follows the device. */
	fun selected(): AppLanguage? =
		AppCompatDelegate.getApplicationLocales()[0]?.let { AppLanguage.fromTag(normalize(it.language)) }

	fun select(language: AppLanguage?) {
		AppCompatDelegate.setApplicationLocales(
			if (language == null) LocaleListCompat.getEmptyLocaleList()
			else LocaleListCompat.forLanguageTags(language.tag)
		)
	}

	/** What the UI and the API are currently using. */
	fun current(): AppLanguage =
		selected() ?: if (normalize(Locale.getDefault().language) == AppLanguage.INDONESIAN.tag) AppLanguage.INDONESIAN
		else AppLanguage.ENGLISH

	fun locale(): Locale = Locale.forLanguageTag(current().tag)

	// Android still reports Indonesian as the old code "in"
	private fun normalize(language: String) = if (language == "in") "id" else language
}
