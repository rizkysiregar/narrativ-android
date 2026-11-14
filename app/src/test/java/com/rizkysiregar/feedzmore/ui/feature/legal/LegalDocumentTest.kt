package com.rizkysiregar.feedzmore.ui.feature.legal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalDocumentTest {

	private val base = "https://api.example.com/api/v1/"

	private fun allowed(url: String) = LegalDocument.isPageUnder(base, url)

	@Test
	fun ownLegalPagesAreAllowed() {
		assertTrue(allowed("https://api.example.com/api/v1/legal/privacy-policy?lang=id&theme=dark"))
		assertTrue(allowed("https://api.example.com/api/v1/legal/terms"))
		assertTrue(allowed("https://API.example.com/api/v1/legal/delete-account?lang=en"))
	}

	@Test
	fun everythingElseIsRefused() {
		assertFalse(allowed("https://evil.example.org/api/v1/legal/terms"))
		assertFalse(allowed("https://api.example.com.evil.org/api/v1/legal/terms"))
		assertFalse(allowed("https://api.example.com@evil.org/api/v1/legal/terms"))
		assertFalse(allowed("https://user:pw@api.example.com/api/v1/legal/terms"))
		assertFalse(allowed("http://api.example.com/api/v1/legal/terms"))
		assertFalse(allowed("https://api.example.com:8443/api/v1/legal/terms"))
		assertFalse(allowed("https://api.example.com/api/v1/posts"))
		assertFalse(allowed("https://api.example.com/api/v1/legal/../posts"))
		assertFalse(allowed("https://api.example.com/legal/terms"))
		assertFalse(allowed("javascript:alert(1)"))
		assertFalse(allowed("file:///etc/passwd"))
		assertFalse(allowed("mailto:help@example.com"))
		assertFalse(allowed("not a url"))
	}

	@Test
	fun theLocalFlavorStaysOnItsOwnHostAndPort() {
		val local = "http://10.0.2.2:9091/api/v1/"
		assertTrue(LegalDocument.isPageUnder(local, "http://10.0.2.2:9091/api/v1/legal/terms"))
		assertFalse(LegalDocument.isPageUnder(local, "http://10.0.2.2:9092/api/v1/legal/terms"))
	}

	@Test
	fun titlesFollowTheLanguage() {
		assertEquals("Kebijakan Privasi", LegalDocument.PRIVACY_POLICY.title("id"))
		assertEquals("Privacy Policy", LegalDocument.PRIVACY_POLICY.title("en"))
		assertEquals("Terms & Conditions", LegalDocument.TERMS.title("en"))
	}
}
