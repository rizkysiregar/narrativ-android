package com.rizkysiregar.feedzmore.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID = Locale.forLanguageTag("id-ID")
private val SAME_YEAR = DateTimeFormatter.ofPattern("d MMM", ID)
private val POST_DATE = DateTimeFormatter.ofPattern("yyyy·MM·dd")
private val OTHER_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", ID)

/**
 * Short relative time for a server timestamp ("baru saja", "5 mnt", "2 jam", "3 hari", "2 mgg",
 * then a date), shown in [zone] (the device zone by default). The server sends an absolute moment
 * with a Z or an offset; a timestamp without any zone comes from an older server that wrote UTC.
 * Returns an empty string when [createdAt] is missing or cannot be parsed.
 */
fun formatTimeAgo(
	createdAt: String?,
	now: LocalDateTime = LocalDateTime.now(),
	zone: ZoneId = ZoneId.systemDefault()
): String {
	val time = parseServerTime(createdAt, zone) ?: return ""
	val seconds = Duration.between(time, now).seconds
	return when {
		seconds < 60 -> "baru saja" // also covers small clock skew (a timestamp slightly in the future)
		seconds < 3_600 -> "${seconds / 60} mnt"
		seconds < 86_400 -> "${seconds / 3_600} jam"
		seconds < 7 * 86_400 -> "${seconds / 86_400} hari"
		seconds < 28 * 86_400 -> "${seconds / (7 * 86_400)} mgg"
		time.year == now.year -> time.format(SAME_YEAR)
		else -> time.format(OTHER_YEAR)
	}
}

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
