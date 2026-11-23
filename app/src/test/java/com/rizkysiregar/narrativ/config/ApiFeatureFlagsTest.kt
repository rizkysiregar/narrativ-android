package com.rizkysiregar.narrativ.config

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Robolectric for SharedPreferences; the plain Application keeps Firebase out of it
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ApiFeatureFlagsTest {
	private lateinit var server: MockWebServer
	private lateinit var retrofit: Retrofit
	private val context: Application get() = ApplicationProvider.getApplicationContext()
	private var answer: MockResponse = ok(chat = true)

	@Before
	fun setUp() {
		server = MockWebServer().apply {
			dispatcher = object : Dispatcher() {
				override fun dispatch(request: RecordedRequest): MockResponse =
					if (request.url.encodedPath == "/api/v1/config/client") answer else MockResponse.Builder().code(404).build()
			}
			start()
		}
		retrofit = Retrofit.Builder().baseUrl(server.url("/api/v1/")).addConverterFactory(GsonConverterFactory.create()).build()
	}

	@After
	fun tearDown() {
		server.close()
	}

	private fun ok(chat: Boolean) = MockResponse.Builder().code(200)
		.body("""{"code":"200","message":"ok","data":{"features":{"chat":$chat}}}""").build()

	private fun flags() = ApiFeatureFlags(retrofit, context)

	@Test fun nothingKnownYetIsOff() {
		assertThat(flags().chatEnabled.value).isFalse()
	}

	@Test fun theAnswerIsUsedAndRememberedForTheNextStart() = runBlocking {
		val flags = flags()
		flags.refresh()
		assertThat(flags.chatEnabled.value).isTrue()

		// a new process starts from the stored answer, before asking the backend
		assertThat(flags().chatEnabled.value).isTrue()

		answer = ok(chat = false)
		flags.refresh()
		assertThat(flags.chatEnabled.value).isFalse()
		assertThat(flags().chatEnabled.value).isFalse()
	}

	@Test fun aFailureKeepsTheLastKnownValue() = runBlocking {
		val flags = flags()
		flags.refresh()
		answer = MockResponse.Builder().code(500).body("""{"code":"500","message":"down","data":null}""").build()

		flags.refresh()

		assertThat(flags.chatEnabled.value).isTrue()
	}

	@Test fun aMissingSwitchIsOff() = runBlocking {
		answer = MockResponse.Builder().code(200).body("""{"code":"200","message":"ok","data":{}}""").build()
		val flags = flags()
		flags.refresh()
		assertThat(flags.chatEnabled.value).isFalse()
	}
}
