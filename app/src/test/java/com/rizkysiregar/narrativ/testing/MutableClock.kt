package com.rizkysiregar.narrativ.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock that only moves when the test says so. */
class MutableClock(private var now: Instant = Instant.parse("2026-11-17T08:00:00Z")) : Clock() {

	fun advanceBy(duration: Duration) {
		now += duration
	}

	override fun instant(): Instant = now

	override fun getZone(): ZoneId = ZoneOffset.UTC

	override fun withZone(zone: ZoneId): Clock = this
}
