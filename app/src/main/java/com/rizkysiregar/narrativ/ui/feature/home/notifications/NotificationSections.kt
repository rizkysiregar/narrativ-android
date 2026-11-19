package com.rizkysiregar.narrativ.ui.feature.home.notifications

import com.rizkysiregar.narrativ.data.remote.model.response.notification.NotificationItem
import com.rizkysiregar.narrativ.util.parseServerTime
import java.time.LocalDate
import java.time.ZoneId

/** Where an entry sits in the inbox list, by the day it happened. */
enum class NotificationSection { TODAY, THIS_WEEK, EARLIER }

internal fun sectionOf(createdAt: String?, today: LocalDate, zone: ZoneId): NotificationSection {
	val day = parseServerTime(createdAt, zone)?.toLocalDate() ?: return NotificationSection.EARLIER
	return when {
		!day.isBefore(today) -> NotificationSection.TODAY
		day.isAfter(today.minusDays(7)) -> NotificationSection.THIS_WEEK
		else -> NotificationSection.EARLIER
	}
}

/** The entries under their section headings, newest first, as the server sends them. */
internal fun groupBySection(
	items: List<NotificationItem>,
	today: LocalDate = LocalDate.now(),
	zone: ZoneId = ZoneId.systemDefault()
): List<Pair<NotificationSection, List<NotificationItem>>> =
	items.groupBy { sectionOf(it.createdAt, today, zone) }
		.toSortedMap()
		.map { (section, entries) -> section to entries }
