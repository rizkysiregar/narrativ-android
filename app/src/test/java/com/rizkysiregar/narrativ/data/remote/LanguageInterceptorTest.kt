package com.rizkysiregar.narrativ.data.remote

import com.google.common.truth.Truth.assertThat
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import java.util.Locale
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric because AppLocale reads the per-app language from AppCompatDelegate.
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class LanguageInterceptorTest {

	private lateinit var server: MockWebServer
	private val client = OkHttpClient.Builder().addInterceptor(LanguageInterceptor()).build()

	@Before
	fun setUp() {
		server = MockWebServer().apply { start() }
		server.enqueue(MockResponse.Builder().code(200).build())
	}

	private val originalLocale = Locale.getDefault()

	@After
	fun tearDown() {
		Locale.setDefault(originalLocale)
		server.close()
	}

	private fun sentLanguage(): String? {
		client.newCall(Request.Builder().url(server.url("/x")).build()).execute().close()
		return server.takeRequest().headers["Accept-Language"]
	}

	// The explicit choice in Settings (AppCompatDelegate) needs a real device or an Activity; what is tested here is the
	// part that does not: with no choice, the app follows the language of the device.
	@Test
	fun aDeviceInIndonesianMakesTheApiAnswerInIndonesian() {
		Locale.setDefault(Locale("in", "ID")) // Android still reports Indonesian with the old code

		assertThat(sentLanguage()).isEqualTo("id")
	}

	@Test
	fun englishIsTheFallbackForADeviceInAnotherLanguage() {
		Locale.setDefault(Locale.FRANCE)

		assertThat(sentLanguage()).isEqualTo("en")
	}
}
