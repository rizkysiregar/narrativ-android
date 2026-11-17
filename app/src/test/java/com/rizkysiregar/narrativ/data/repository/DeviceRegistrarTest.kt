package com.rizkysiregar.narrativ.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.cache.JwtTokenHolder
import com.rizkysiregar.narrativ.testing.MutableClock
import com.rizkysiregar.narrativ.testing.fakes.FakePushTokenProvider
import com.rizkysiregar.narrativ.testing.preferencesFile
import com.rizkysiregar.narrativ.testing.testDataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class DeviceRegistrarTest {

	@get:Rule
	val folder = TemporaryFolder()

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val tokenHolder = JwtTokenHolder()
	private val pushTokens = FakePushTokenProvider()
	private val clock = MutableClock()

	private lateinit var server: MockWebServer
	private lateinit var dataStore: DataStoreManager
	private lateinit var registrar: DeviceRegistrar

	@Before
	fun setUp() {
		server = MockWebServer().apply { start() }
		dataStore = testDataStoreManager(ioScope, folder.preferencesFile())
		val retrofit = Retrofit.Builder().baseUrl(server.url("/api/v1/")).addConverterFactory(GsonConverterFactory.create()).build()
		registrar = ApiDeviceRegistrar(context, dataStore, tokenHolder, pushTokens, clock, retrofit)
		server.enqueue(MockResponse.Builder().code(200).build())
	}

	@After
	fun tearDown() {
		server.close()
		ioScope.cancel()
	}

	private fun signIn(userId: String = "u1") = runBlocking {
		dataStore.put(PrefKeys.isLoggedIn, true)
		dataStore.put(PrefKeys.userId, userId)
		dataStore.put(PrefKeys.token, "jwt-1")
	}

	private fun sync(force: Boolean = false) = runBlocking { registrar.sync(force) }

	private fun enqueueOk(times: Int = 1) = repeat(times) { server.enqueue(MockResponse.Builder().code(200).build()) }

	// --- nothing to do ---

	@Test
	fun nothingIsSentWhenNobodyIsSignedIn() {
		assertThat(sync()).isTrue()

		assertThat(server.requestCount).isEqualTo(0)
		assertThat(pushTokens.tokenCalls).isEqualTo(0)
	}

	@Test
	fun nothingIsSentWithoutTheStoredJwtOrUserId() {
		runBlocking { dataStore.put(PrefKeys.isLoggedIn, true) }

		assertThat(sync()).isTrue()

		assertThat(server.requestCount).isEqualTo(0)
	}

	// --- registering ---

	@Test
	fun aSignedInDeviceIsRegisteredAndRemembered() {
		signIn("u1")

		assertThat(sync()).isTrue()

		val request = server.takeRequest()
		assertThat(request.method).isEqualTo("POST")
		assertThat(request.url.encodedPath).isEqualTo("/api/v1/devices")
		val body = request.body!!.utf8()
		assertThat(body).contains("\"pushToken\":\"fcm-token-1\"")
		assertThat(body).contains("\"platform\":\"ANDROID\"")
		assertThat(body).contains("\"timezone\":")
		assertThat(body).contains("\"locale\":")
		runBlocking {
			assertThat(dataStore.get(PrefKeys.deviceSyncToken, "").first()).isEqualTo("fcm-token-1")
			assertThat(dataStore.get(PrefKeys.deviceSyncUserId, "").first()).isEqualTo("u1")
			assertThat(dataStore.get(PrefKeys.deviceSyncAt, 0L).first()).isEqualTo(clock.millis())
		}
	}

	@Test
	fun theWorkerMayStartBeforeTheSplashLoadedTheJwtSoItIsLoadedHere() {
		signIn()
		assertThat(tokenHolder.getToken()).isNull()

		sync()

		assertThat(tokenHolder.getToken()).isEqualTo("jwt-1")
	}

	// --- throttle ---

	@Test
	fun aSecondSyncWithinADayWithTheSameTokenAndUserSendsNothing() {
		signIn()
		sync()

		clock.advanceBy(Duration.ofHours(23))
		assertThat(sync()).isTrue()

		assertThat(server.requestCount).isEqualTo(1)
	}

	@Test
	fun afterADayItIsSentAgain() {
		signIn()
		sync()
		enqueueOk()

		clock.advanceBy(Duration.ofHours(24))
		sync()

		assertThat(server.requestCount).isEqualTo(2)
	}

	@Test
	fun aNewPushTokenIsSentRightAway() {
		signIn()
		sync()
		enqueueOk()
		pushTokens.onToken = { "fcm-token-2" }

		sync()

		assertThat(server.requestCount).isEqualTo(2)
	}

	@Test
	fun anotherAccountOnTheSameDeviceIsSentRightAway() {
		signIn("u1")
		sync()
		enqueueOk()
		signIn("u2")

		sync()

		assertThat(server.requestCount).isEqualTo(2)
	}

	@Test
	fun forceSkipsTheThrottle() {
		signIn()
		sync()
		enqueueOk()

		sync(force = true)

		assertThat(server.requestCount).isEqualTo(2)
	}

	// --- failures ---

	@Test
	fun whateverTheServerAnswersTheSyncCountsAsDone() {
		signIn()
		server.dispatcher = object : mockwebserver3.Dispatcher() {
			override fun dispatch(request: mockwebserver3.RecordedRequest) = MockResponse.Builder().code(500).build()
		}

		assertThat(sync()).isTrue() // documented: only failing to reach the server is retried

		runBlocking { assertThat(dataStore.get(PrefKeys.deviceSyncToken, "").first()).isEqualTo("fcm-token-1") }
	}

	@Test
	fun notReachingTheServerAsksForARetryAndRemembersNothing() {
		signIn()
		server.dispatcher = object : mockwebserver3.Dispatcher() {
			override fun dispatch(request: mockwebserver3.RecordedRequest) =
				MockResponse.Builder().onResponseStart(SocketEffect.ShutdownConnection).build()
		}

		assertThat(sync()).isFalse()

		runBlocking { assertThat(dataStore.get(PrefKeys.deviceSyncToken, "").first()).isEmpty() }
	}

	@Test
	fun noPushTokenAsksForARetry() {
		signIn()
		pushTokens.onToken = { throw IllegalStateException("no play services") }

		assertThat(sync()).isFalse()

		assertThat(server.requestCount).isEqualTo(0)
	}
}
