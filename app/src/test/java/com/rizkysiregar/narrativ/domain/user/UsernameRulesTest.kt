package com.rizkysiregar.narrativ.domain.user

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsernameRulesTest {

	@Test
	fun lettersDigitsDotsAndUnderscoresAreAccepted() {
		listOf("rizky", "Rizky_99", "r.siregar", "abc", "a".repeat(16)).forEach {
			assertTrue(it, UsernameRules.isValid(it))
		}
	}

	// the same rule as the backend, so the form never lets through what the server answers with a 400
	@Test
	fun everythingElseIsRefused() {
		listOf("ab", "a".repeat(17), "has space", "a/b", "rízky", "rizky!", "../x", "").forEach {
			assertFalse(it, UsernameRules.isValid(it))
		}
	}
}
