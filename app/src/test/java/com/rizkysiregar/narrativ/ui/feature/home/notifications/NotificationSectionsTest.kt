package com.rizkysiregar.narrativ.ui.feature.home.notifications

import com.rizkysiregar.narrativ.data.remote.model.response.notification.NotificationItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NotificationSectionsTest {

	private val zone = ZoneId.of("Asia/Jakarta")
	private val today = LocalDate.of(2026, 11, 19)

	private fun item(key: String, createdAt: String) = NotificationItem(groupKey = key, type = "POST_LIKED", createdAt = createdAt)

	@Test
	fun entriesFallUnderTodayThisWeekAndEarlier() {
		val sections = groupBySection(
			listOf(
				item("a", "2026-11-19T01:00:00Z"),  // 08:00 in Jakarta, today
				item("b", "2026-11-18T23:30:00Z"),  // 06:30 on the 19th in Jakarta, still today
				item("c", "2026-11-15T10:00:00Z"),
				item("d", "2026-11-01T10:00:00Z")
			),
			today, zone
		)

		assertEquals(
			listOf(
				NotificationSection.TODAY to listOf("a", "b"),
				NotificationSection.THIS_WEEK to listOf("c"),
				NotificationSection.EARLIER to listOf("d")
			),
			sections.map { (section, items) -> section to items.map { it.groupKey } }
		)
	}

	@Test
	fun anEntryWithoutATimeGoesUnderEarlier() {
		assertEquals(NotificationSection.EARLIER, sectionOf(null, today, zone))
	}
}
