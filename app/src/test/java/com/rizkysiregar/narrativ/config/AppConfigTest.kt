package com.rizkysiregar.narrativ.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigTest {

	@Test
	fun `defaults block no version`() {
		assertFalse(AppConfig().requiresUpdate(currentVersionCode = 1))
	}

	@Test
	fun `a version below the minimum must update`() {
		assertTrue(AppConfig(minSupportedVersionCode = 5).requiresUpdate(currentVersionCode = 4))
	}

	@Test
	fun `the minimum version itself may keep running`() {
		assertFalse(AppConfig(minSupportedVersionCode = 5).requiresUpdate(currentVersionCode = 5))
	}

	@Test
	fun `a newer version may keep running`() {
		assertFalse(AppConfig(minSupportedVersionCode = 5).requiresUpdate(currentVersionCode = 9))
	}
}
