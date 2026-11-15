package com.rizkysiregar.feedzmore.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.rizkysiregar.feedzmore.i18n.AppLanguage
import com.rizkysiregar.feedzmore.i18n.AppLocale

private val POST_DATE = DateTimeFormatter.ofPattern("yyyy·MM·dd")

/** The words of one language; plain text instead of resources so the function stays free of Android. */
private class TimeWords(val justNow: String, val minute: String, val hour: String, val day: String, val week: String, val locale: Locale)

private val INDONESIAN = TimeWords("baru saja", "mnt", "jam", "hari", "mgg", Locale.forLanguageTag("id-ID"))
private val ENGLISH = TimeWords("just now", "m", "h", "d", "w", Locale.ENGLISH)

private fun wordsFor(language: AppLanguage) = if (language == AppLanguage.INDONESIAN) INDONESIAN else ENGLISH

/**
 * Short relative time for a server timestamp in [language] ("baru saja", "5 mnt", "2 jam", "3 hari", "2 mgg"
 * or "just now", "5m", "2h", "3d", "2w", then a date), shown in [zone] (the device zone by default). The server sends an absolute moment
 * with a Z or an offset; a timestamp without any zone comes from an older server that wrote UTC.
 * Returns an empty string when [createdAt] is missing or cannot be parsed.
 */
fun formatTimeAgo(
	createdAt: String?,
	now: LocalDateTime = LocalDateTime.now(),
	zone: ZoneId = ZoneId.systemDefault(),
	language: AppLanguage = AppLocale.current()
): String {
	val time = parseServerTime(createdAt, zone) ?: return ""
	val words = wordsFor(language)
	val seconds = Duration.between(time, now).seconds
	return when {
		seconds < 60 -> words.justNow // also covers small clock skew (a timestamp slightly in the future)
		seconds < 3_600 -> "${seconds / 60}${spaceFor(language)}${words.minute}"
		seconds < 86_400 -> "${seconds / 3_600}${spaceFor(language)}${words.hour}"
		seconds < 7 * 86_400 -> "${seconds / 86_400}${spaceFor(language)}${words.day}"
		seconds < 28 * 86_400 -> "${seconds / (7 * 86_400)}${spaceFor(language)}${words.week}"
		time.year == now.year -> time.format(DateTimeFormatter.ofPattern("d MMM", words.locale))
		else -> time.format(DateTimeFormatter.ofPattern("d MMM yyyy", words.locale))
	}
}

// "5 mnt" in Indonesian, "5m" in English
private fun spaceFor(language: AppLanguage) = if (language == AppLanguage.INDONESIAN) " " else ""

internal fun parseServerTime(value: String?, zone: ZoneId): LocalDateTime? {
	if (value.isNullOrBlank()) return null
	val instant = runCatching { Instant.parse(value) }
		.recoverCatching { DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(value, Instant::from) }
		.recoverCatching { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }
		.getOrNull() ?: return null
	return LocalDateTime.ofInstant(instant, zone)
}

/**
 * Date of a post for the card, as "2026·11·11" in [zone] (the device zone by default). A value that is
 * not a full timestamp (a bare date) is shown as it is.
 */
fun formatPostDate(createdAt: String, zone: ZoneId = ZoneId.systemDefault()): String {
	val time = parseServerTime(createdAt, zone)
	return if (time != null) time.format(POST_DATE) else createdAt.take(10).replace('-', '·')
}
