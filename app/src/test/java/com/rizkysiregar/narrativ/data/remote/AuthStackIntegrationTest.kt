package com.rizkysiregar.narrativ.data.remote

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.cache.JwtTokenHolder
import com.rizkysiregar.narrativ.data.session.SessionManager
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.testing.fakes.FakeAppAnalytics
import com.rizkysiregar.narrativ.testing.fakes.FakeCrashReporter
import com.rizkysiregar.narrativ.testing.preferencesFile
import com.rizkysiregar.narrativ.testing.testDataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The real OkHttp stack of the app (AuthInterceptor + TokenRefresher + SessionManager + ApiErrorManager) against
 * a MockWebServer, since the bugs that matter here live in how these pieces work together.
 */
class AuthStackIntegrationTest {

	@get:Rule
	val folder = TemporaryFolder()

	private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val tokenHolder = JwtTokenHolder()
	private val analytics = FakeAppAnalytics()
	private val apiErrors = ApiErrorManager()
	private val reported = CopyOnWriteArrayList<ApiErrorEvent>()
	private val seen = CopyOnWriteArrayList<String>()

	private lateinit var server: MockWebServer
	private lateinit var dataStore: DataStoreManager
	private lateinit var client: OkHttpClient

	@Before
	fun setUp() {
		server = MockWebServer().apply { start() }
		dataStore = testDataStoreManager(ioScope, folder.preferencesFile())
		runBlocking {
			dataStore.put(PrefKeys.token, OLD_ACCESS)
			dataStore.put(PrefKeys.refreshToken, "refresh-1")
		}
		tokenHolder.setToken(OLD_ACCESS)

		val sessionManager = SessionManager(dataStore, tokenHolder, analytics, FakeCrashReporter(), ioScope)
		sessionManagerRef = sessionManager
		val refresher = TokenRefresher(dataStore, tokenHolder, OkHttpClient(), server.url("/api/v1/").toString())
		client = OkHttpClient.Builder()
			.addInterceptor(AuthInterceptor(tokenHolder, sessionManager, apiErrors, refresher))
			.build()

		// subscribe before any request, the channel keeps nothing for late collectors
		ioScope.launch(start = CoroutineStart.UNDISPATCHED) { apiErrors.errors.collect { reported += it } }
	}

	private lateinit var sessionManagerRef: SessionManager

	@After
	fun tearDown() {
		server.close()
		ioScope.cancel()
	}

	// --- helpers ---

	private fun serve(handler: (RecordedRequest) -> MockResponse) {
		server.dispatcher = object : Dispatcher() {
			override fun dispatch(request: RecordedRequest): MockResponse {
				seen += "${request.method} ${request.url.encodedPath}"
				return handler(request)
			}
		}
	}

	private fun json(code: Int, body: String) =
		MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

	private fun loginBody(access: String, refresh: String) =
		"""{"code":"200","message":"OK","data":{"token":"$access","refreshToken":"$refresh","username":"rizky","userId":"u1"}}"""

	private fun refreshOk(delayMs: Long = 0) =
		MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
			.body(loginBody(NEW_ACCESS, "refresh-2")).headersDelay(delayMs, TimeUnit.MILLISECONDS).build()

	private fun dropConnection() = MockResponse.Builder().onResponseStart(SocketEffect.ShutdownConnection).build()

	private fun isRefresh(r: RecordedRequest) = r.url.encodedPath.endsWith("/auth/refresh")

	private fun bearer(r: RecordedRequest) = r.headers["Authorization"]

	private fun get(path: String = "/api/v1/posts"): Response =
		client.newCall(Request.Builder().url(server.url(path)).build()).execute()

	private fun awaitExpiredMessage(): String = runBlocking {
		withTimeout(5_000) { sessionManagerRef.sessionExpiredMessage.first { it != null }!! }
	}

	private fun awaitReported(count: Int = 1): List<ApiErrorEvent> {
		val deadline = System.currentTimeMillis() + 3_000
		while (reported.size < count && System.currentTimeMillis() < deadline) Thread.sleep(20)
		return reported.toList()
	}

	private fun assertNothingReported() {
		Thread.sleep(300) // the collector runs on another thread
		assertThat(reported).isEmpty()
	}

	private fun assertSessionStillAlive() {
		assertThat(tokenHolder.getToken()).isEqualTo(OLD_ACCESS)
		assertThat(analytics.names()).doesNotContain(AnalyticsEvents.SESSION_EXPIRED)
	}

	// --- headers ---

	@Test
	fun aProtectedRequestCarriesTheBearerToken() {
		serve { json(200, "{}") }

		get("/api/v1/posts").close()

		assertThat(server.takeRequest().headers["Authorization"]).isEqualTo("Bearer $OLD_ACCESS")
	}

	@Test
	fun authEndpointsNeverCarryTheStoredToken() {
		serve { json(200, "{}") }

		get("/api/v1/auth/login").close()

		assertThat(server.takeRequest().headers["Authorization"]).isNull()
	}

	// --- 401 handling ---

	@Test
	fun a401IsRefreshedAndTheRequestRepeatedWithTheNewToken() {
		serve { r ->
			when {
				isRefresh(r) -> refreshOk()
				bearer(r) == "Bearer $NEW_ACCESS" -> json(200, """{"ok":true}""")
				else -> json(401, """{"message":"expired"}""")
			}
		}

		val response = get()

		assertThat(response.code).isEqualTo(200)
		assertThat(seen).containsExactly("GET /api/v1/posts", "POST /api/v1/auth/refresh", "GET /api/v1/posts").inOrder()
		assertThat(tokenHolder.getToken()).isEqualTo(NEW_ACCESS)
		runBlocking {
			assertThat(dataStore.get(PrefKeys.token, "").first()).isEqualTo(NEW_ACCESS)
			assertThat(dataStore.get(PrefKeys.refreshToken, "").first()).isEqualTo("refresh-2")
		}
		assertThat(analytics.names()).doesNotContain(AnalyticsEvents.SESSION_EXPIRED)
		assertNothingReported()
	}

	@Test
	fun theRefreshCallSendsTheRefreshTokenAndNoAccessToken() {
		serve { r -> if (isRefresh(r)) refreshOk() else if (bearer(r) == "Bearer $NEW_ACCESS") json(200, "{}") else json(401, "{}") }

		get().close()

		server.takeRequest() // the original request
		val refresh = server.takeRequest()
		assertThat(refresh.body!!.utf8()).contains("refresh-1")
		assertThat(refresh.headers["Authorization"]).isNull()
	}

	@Test
	fun ifTheRepeatedRequestIsRejectedAgainTheSessionEnds() {
		serve { r -> if (isRefresh(r)) refreshOk() else json(401, """{"message":"Token tidak valid"}""") }

		val response = get()

		assertThat(response.code).isEqualTo(401)
		assertThat(awaitExpiredMessage()).isEqualTo("Token tidak valid")
		assertThat(tokenHolder.getToken()).isNull()
		assertThat(analytics.names()).containsExactly(AnalyticsEvents.SESSION_EXPIRED)
	}

	@Test
	fun aRejectedRefreshEndsTheSessionWithTheMessageOfTheOriginal401() {
		serve { r -> if (isRefresh(r)) json(401, """{"message":"refresh habis"}""") else json(401, """{"message":"Sesi habis"}""") }

		get().close()

		assertThat(awaitExpiredMessage()).isEqualTo("Sesi habis")
		assertThat(tokenHolder.getToken()).isNull()
		runBlocking { assertThat(dataStore.get(PrefKeys.refreshToken, "").first()).isEmpty() }
	}

	@Test
	fun noStoredRefreshTokenEndsTheSessionWithoutCallingTheServer() {
		runBlocking { dataStore.delete(PrefKeys.refreshToken) }
		serve { json(401, """{"message":"Sesi habis"}""") }

		get().close()

		assertThat(awaitExpiredMessage()).isEqualTo("Sesi habis")
		assertThat(seen).containsExactly("GET /api/v1/posts")
	}

	@Test
	fun aServerErrorWhileRefreshingKeepsTheSessionAndPassesTheOriginal401On() {
		serve { r -> if (isRefresh(r)) json(500, """{"message":"down"}""") else json(401, """{"message":"expired"}""") }

		val response = get()

		assertThat(response.code).isEqualTo(401)
		assertSessionStillAlive()
	}

	@Test
	fun aNetworkFailureWhileRefreshingKeepsTheSession() {
		serve { r ->
			if (isRefresh(r)) dropConnection()
			else json(401, """{"message":"expired"}""")
		}

		val response = get()

		assertThat(response.code).isEqualTo(401)
		assertSessionStillAlive()
	}

	@Test
	fun parallelRequestsThatAllGetA401TriggerASingleRefresh() {
		serve { r ->
			when {
				isRefresh(r) -> refreshOk(delayMs = 300)
				bearer(r) == "Bearer $NEW_ACCESS" -> json(200, "{}")
				else -> json(401, """{"message":"expired"}""")
			}
		}
		val threads = 5
		val pool = Executors.newFixedThreadPool(threads)
		val start = CountDownLatch(1)
		val codes = CopyOnWriteArrayList<Int>()
		val done = CountDownLatch(threads)

		repeat(threads) {
			pool.execute {
				start.await()
				get().use { codes += it.code }
				done.countDown()
			}
		}
		start.countDown()
		assertThat(done.await(15, TimeUnit.SECONDS)).isTrue()
		pool.shutdown()

		assertThat(codes).containsExactly(200, 200, 200, 200, 200)
		assertThat(seen.count { it == "POST /api/v1/auth/refresh" }).isEqualTo(1)
		assertThat(analytics.names()).doesNotContain(AnalyticsEvents.SESSION_EXPIRED)
	}

	@Test
	fun aWrongPasswordIsNeitherRefreshedNorReportedNorEndsTheSession() {
		serve { json(401, """{"message":"Password salah"}""") }

		val response = get("/api/v1/auth/login")

		assertThat(response.code).isEqualTo(401)
		assertThat(response.body!!.string()).contains("Password salah") // the screen still reads the body
		assertThat(seen).containsExactly("GET /api/v1/auth/login")
		assertSessionStillAlive()
		assertNothingReported()
	}

	// --- what reaches the global error dialog ---

	@Test
	fun aSuccessAndA404AreNotReported() {
		serve { r -> if (r.url.encodedPath.endsWith("/missing")) json(404, "{}") else json(200, "{}") }

		get("/api/v1/posts").close()
		get("/api/v1/missing").close()

		assertNothingReported()
	}

	@Test
	fun aServerErrorIsReportedWithTheMessageOfTheServer() {
		serve { json(500, """{"message":"DB down"}""") }

		get().close()

		assertThat(awaitReported()).containsExactly(ApiErrorEvent(500, "DB down"))
	}

	@Test
	fun aServerErrorWithoutAMessageIsReportedAsGeneric() {
		serve { json(502, "<html>bad gateway</html>") }

		get().close()

		assertThat(awaitReported()).containsExactly(ApiErrorEvent(502, ApiMessages.GENERIC_ERROR))
	}

	@Test
	fun aNetworkFailureIsReportedAndStillThrown() {
		serve { dropConnection() }

		var thrown: IOException? = null
		try {
			get().close()
		} catch (e: IOException) {
			thrown = e
		}

		assertThat(thrown).isNotNull()
		assertThat(awaitReported()).containsExactly(ApiErrorEvent(null, ApiMessages.NETWORK_ERROR))
	}

	@Test
	fun aCancelledCallIsNotReported() {
		serve { MockResponse.Builder().code(200).headersDelay(2, TimeUnit.SECONDS).build() }
		val failed = CountDownLatch(1)
		val call = client.newCall(Request.Builder().url(server.url("/api/v1/posts")).build())

		call.enqueue(object : okhttp3.Callback {
			override fun onFailure(call: okhttp3.Call, e: IOException) = failed.countDown()
			override fun onResponse(call: okhttp3.Call, response: Response) = response.close()
		})
		Thread.sleep(200)
		call.cancel()

		assertThat(failed.await(5, TimeUnit.SECONDS)).isTrue()
		assertNothingReported()
	}

	/**
	 * R6 in the test plan: every call through this client reports its failure to the global error dialog, including
	 * the ones that are meant to be silent (DeviceRegistrar, RemoteSessionCleaner use the same Retrofit). This pins
	 * what happens today; when those calls get an opt-out, flip the assertion.
	 */
	@Test
	fun aFailingBackgroundCallStillReachesTheGlobalErrorChannel() {
		serve { json(503, """{"message":"maintenance"}""") }

		client.newCall(
			Request.Builder().url(server.url("/api/v1/devices")).post("{}".toRequestBody()).build()
		).execute().close()

		assertThat(awaitReported()).containsExactly(ApiErrorEvent(503, "maintenance"))
	}

	private companion object {
		const val OLD_ACCESS = "old-access"
		const val NEW_ACCESS = "new-access"
	}
}
