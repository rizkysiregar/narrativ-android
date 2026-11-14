package com.rizkysiregar.feedzmore.domain.moderation

import com.rizkysiregar.feedzmore.data.remote.model.request.user.DeleteAccountRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportReasonTest {

	// the server parses these names as enums, so a rename here would turn every report into a 400
	@Test
	fun apiNamesMatchTheServerEnums() {
		assertEquals(
			listOf("SPAM", "HARASSMENT", "HATE_SPEECH", "SEXUAL_CONTENT", "VIOLENCE", "MISINFORMATION", "OTHER"),
			ReportReason.entries.map { it.apiName }
		)
		assertEquals(listOf("POST", "COMMENT", "USER"), ReportTarget.entries.map { it.apiName })
	}

	@Test
	fun onlyOtherNeedsADescription() {
		assertTrue(ReportReason.OTHER.needsDescription)
		assertFalse(ReportReason.SPAM.needsDescription)
	}

	@Test
	fun deleteAccountRequestNeverPrintsTheCredential() {
		val text = DeleteAccountRequest(password = "hunter2", idToken = "secret-token").toString()

		assertFalse(text.contains("hunter2"))
		assertFalse(text.contains("secret-token"))
	}
}
