package com.rizkysiregar.feedzmore.util

import com.rizkysiregar.feedzmore.i18n.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TimeAgoTest {

	private val now = LocalDateTime.of(2026, 11, 11, 12, 0, 0)
	private val utc = ZoneId.of("UTC")
	private val wib = ZoneId.of("Asia/Jakarta")

	private fun ago(createdAt: String?, zone: ZoneId = utc, language: AppLanguage = AppLanguage.INDONESIAN) =
		formatTimeAgo(createdAt, now, zone, language)

	@Test
	fun missingOrBrokenTimestampGivesNothing() {
		assertEquals("", ago(null))
		assertEquals("", ago(""))
		assertEquals("", ago("kemarin"))
	}

	@Test
	fun lessThanAMinuteAndFutureTimestampsAreJustNow() {
		assertEquals("baru saja", ago("2026-11-11T11:59:30Z"))
		assertEquals("baru saja", ago("2026-11-11T12:00:05Z"))
	}

	@Test
	fun minutesHoursDaysAndWeeks() {
		assertEquals("5 mnt", ago("2026-11-11T11:54:00.123456Z"))
		assertEquals("2 jam", ago("2026-11-11T10:00:00Z"))
		assertEquals("3 hari", ago("2026-11-08T12:00:00Z"))
		assertEquals("2 mgg", ago("2026-10-28T12:00:00Z"))
	}

	@Test
	fun olderThanAMonthShowsTheDate() {
		assertEquals("5 Sep", ago("2026-09-05T08:00:00Z"))
		assertEquals("5 Sep 2025", ago("2025-09-05T08:00:00Z"))
	}

	@Test
	fun zUtcTimestampIsShownInTheDeviceZone() {
		// 04:55 UTC is 11:55 WIB, five minutes before "now" (12:00 WIB)
		assertEquals("5 mnt", ago("2026-11-11T04:55:00Z", wib))
		assertEquals("5 mnt", ago("2026-11-11T11:55:00+07:00", wib))
	}

	@Test
	fun theSameMomentReadsTheSameInEveryZone() {
		// the bug: a 03:31 UTC comment was shown as if it was 03:31 local time, 7 hours too old in WIB
		assertEquals("28 mnt", formatTimeAgo("2026-11-11T03:31:06Z", LocalDateTime.of(2026, 11, 11, 11, 0, 0), wib, AppLanguage.INDONESIAN))
	}

	@Test
	fun timestampWithoutAZoneIsReadAsUtc() {
		assertEquals("5 mnt", ago("2026-11-11T04:55:00", wib))
		assertEquals("baru saja", ago("2026-11-11T05:00:00.123456", wib))
	}

	@Test
	fun postDateIsTheDayInTheDeviceZone() {
		// 20:00 UTC is already the next day in WIB
		assertEquals("2026·11·12", formatPostDate("2026-11-11T20:00:00Z", wib))
		assertEquals("2026·11·11", formatPostDate("2026-11-11T20:00:00Z", utc))
	}

	@Test
	fun postDateKeepsABareDateAsItIs() {
		assertEquals("2026·10·05", formatPostDate("2026-10-05", wib))
	}

	@Test
	fun `english uses short suffixes and english month names`() {
		assertEquals("just now", ago("2026-11-11T11:59:30Z", language = AppLanguage.ENGLISH))
		assertEquals("5m", ago("2026-11-11T11:55:00Z", language = AppLanguage.ENGLISH))
		assertEquals("2h", ago("2026-11-11T10:00:00Z", language = AppLanguage.ENGLISH))
		assertEquals("3d", ago("2026-11-08T12:00:00Z", language = AppLanguage.ENGLISH))
		assertEquals("2w", ago("2026-10-28T12:00:00Z", language = AppLanguage.ENGLISH))
	}
}
