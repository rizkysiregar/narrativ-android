package com.rizkysiregar.feedzmore.ui.feature.legal

import com.rizkysiregar.feedzmore.BuildConfig
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/**
 * The public pages the backend serves under `legal/`: the same ones the Play Console links to, so the text
 * lives in one place and changes without a new app release.
 */
enum class LegalDocument(val path: String, val titleId: String, val titleEn: String) {
	PRIVACY_POLICY("privacy-policy", "Kebijakan Privasi", "Privacy Policy"),
	TERMS("terms", "Syarat & Ketentuan", "Terms & Conditions");

	fun title(language: String): String = if (language == LANGUAGE_ID) titleId else titleEn

	/** The page in [language], always dark: the app has no light theme. */
	fun url(language: String): String =
		"${BuildConfig.BASE_URL}$PAGES_PATH$path?lang=$language&theme=dark"

	companion object {
		const val LANGUAGE_ID = "id"
		const val LANGUAGE_EN = "en"
		private const val PAGES_PATH = "legal/"

		fun fromPath(path: String?): LegalDocument? = entries.firstOrNull { it.path == path }

		/** Indonesian for an Indonesian device, English for everything else. */
		fun deviceLanguage(): String = when (Locale.getDefault().language) {
			"in", "id" -> LANGUAGE_ID
			else -> LANGUAGE_EN
		}

		/** Whether [url] is one of the legal pages of our own server, the only thing the WebView may show. */
		fun isOwnPage(url: String): Boolean = isPageUnder(BuildConfig.BASE_URL, url)

		/**
		 * [url] must share scheme, host and port with [baseUrl] and sit under its `legal/` path. The path is
		 * normalized first, so `legal/../posts` does not pass, and an unparsable url never does.
		 */
		internal fun isPageUnder(baseUrl: String, url: String): Boolean = try {
			val base = URI(baseUrl)
			val target = URI(url).normalize()
			target.scheme.equals(base.scheme, ignoreCase = true) &&
				target.host.equals(base.host, ignoreCase = true) &&
				target.port == base.port &&
				target.rawUserInfo == null &&
				target.path.orEmpty().startsWith("${base.path.orEmpty()}$PAGES_PATH")
		} catch (_: URISyntaxException) {
			false
		}
	}
}
