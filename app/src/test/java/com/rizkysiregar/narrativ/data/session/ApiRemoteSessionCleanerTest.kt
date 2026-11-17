package com.rizkysiregar.narrativ.data.session

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.testing.fakes.FakePushTokenProvider
import com.rizkysiregar.narrativ.testing.preferencesFile
import com.rizkysiregar.narrativ.testing.testDataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class ApiRemoteSessionCleanerTest {

	@get:Rule
	val folder = TemporaryFolder()

	private val pushTokens = FakePushTokenProvider()
	private val seen = CopyOnWriteArrayList<String>()
	private val bodies = CopyOnWriteArrayList<String>()
	private var answer: (RecordedRequest) -> MockResponse = { MockResponse.Builder().code(200).body("""{"code":"200","message":"ok","data":null}""").build() }
	private lateinit var server: MockWebServer
	private lateinit var retrofit: Retrofit

	@Before
	fun setUp() {
		server = MockWebServer().apply {
			dispatcher = object : Dispatcher() {
				override fun dispatch(request: RecordedRequest): MockResponse {
					seen += request.url.encodedPath
					bodies += request.body?.utf8().orEmpty()
					return answer(request)
				}
			}
			start()
		}
		retrofit = Retrofit.Builder().baseUrl(server.url("/api/v1/")).addConverterFactory(GsonConverterFactory.create()).build()
	}

	@After
	fun tearDown() {
		server.close()
		ioScope.cancel()
	}

	// The tests that talk to the server run on real time: under runTest the 8 second timeout would fire on
	// virtual time while the answer of the (real) server is still on its way.
	private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	private suspend fun realTimeCleaner(refreshToken: String? = "refresh-1"): ApiRemoteSessionCleaner {
		val dataStore = testDataStoreManager(ioScope, folder.preferencesFile())
		if (refreshToken != null) dataStore.put(PrefKeys.refreshToken, refreshToken)
		return ApiRemoteSessionCleaner(dataStore, pushTokens, retrofit)
	}

	private suspend fun TestScope.cleaner(refreshToken: String? = "refresh-1"): ApiRemoteSessionCleaner {
		val dataStore = testDataStoreManager(backgroundScope, folder.preferencesFile())
		if (refreshToken != null) dataStore.put(PrefKeys.refreshToken, refreshToken)
		return ApiRemoteSessionCleaner(dataStore, pushTokens, retrofit)
	}

	@Test
	fun theDeviceIsDeactivatedFirstThenTheRefreshTokenIsRevokedThenThePushTokenIsDropped() = runBlocking<Unit> {
		realTimeCleaner().signOut()

		assertThat(seen).containsExactly("/api/v1/devices/deactivate", "/api/v1/auth/logout").inOrder()
		assertThat(bodies[0]).contains("fcm-token-1")
		assertThat(bodies[1]).contains("refresh-1")
		assertThat(pushTokens.deleteCalls).isEqualTo(1)
	}

	@Test
	fun withoutAStoredRefreshTokenThereIsNothingToRevoke() = runBlocking<Unit> {
		realTimeCleaner(refreshToken = null).signOut()

		assertThat(seen).containsExactly("/api/v1/devices/deactivate")
		assertThat(pushTokens.deleteCalls).isEqualTo(1)
	}

	@Test
	fun aFailedStepDoesNotStopTheNextOnes() = runBlocking<Unit> {
		answer = { r ->
			if (r.url.encodedPath.endsWith("deactivate")) MockResponse.Builder().code(500).build()
			else MockResponse.Builder().code(200).body("""{"code":"200","message":"ok","data":null}""").build()
		}

		realTimeCleaner().signOut()

		assertThat(seen).containsExactly("/api/v1/devices/deactivate", "/api/v1/auth/logout").inOrder()
		assertThat(pushTokens.deleteCalls).isEqualTo(1)
	}

	@Test
	fun noPushTokenStillRevokesTheRefreshToken() = runBlocking<Unit> {
		pushTokens.onToken = { throw IllegalStateException("no play services") }

		realTimeCleaner().signOut()

		assertThat(seen).containsExactly("/api/v1/auth/logout")
	}

	@Test
	fun aUserWithoutAConnectionIsNotKeptWaiting() = runTest {
		pushTokens.onToken = { awaitCancellation() } // the token never arrives

		cleaner().signOut() // returns after the 8 seconds, on virtual time

		assertThat(seen).isEmpty()
		assertThat(pushTokens.deleteCalls).isEqualTo(1) // still dropped, so nothing is sent to this device
	}

	@Test
	fun droppingThePushTokenAloneDeletesIt() = runTest {
		cleaner().dropPushToken()

		assertThat(pushTokens.deleteCalls).isEqualTo(1)
		assertThat(seen).isEmpty()
	}
}
