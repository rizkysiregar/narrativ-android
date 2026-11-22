package com.rizkysiregar.narrativ.chat

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.marpokat.TokenProvider
import com.rizkysiregar.narrativ.data.remote.model.response.ChatTokenResponse
import com.rizkysiregar.narrativ.testing.fakes.FakeChatBackend
import com.rizkysiregar.narrativ.testing.fakes.FakePushTokenProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class MarpokatChatSessionTest {
	private lateinit var server: MockWebServer
	private lateinit var retrofit: Retrofit
	private val backend = FakeChatBackend()
	private val pushTokens = FakePushTokenProvider()
	private val stopped = mutableListOf<String>()
	private val created = mutableListOf<ChatTokenResponse>()
	private val firstTokens = mutableListOf<String>()
	private var tokenStatus = 200
	private var dmStatus = 200
	private var tokenCalls = 0

	private val factory = object : ChatClientFactory {
		override fun create(credentials: ChatTokenResponse, tokenProvider: TokenProvider): ChatConnection {
			created += credentials
			return ChatConnection(backend.client(Dispatchers.Default)) { stopped += "stopped" }
		}
	}

	@Before
	fun setUp() {
		server = MockWebServer().apply {
			dispatcher = object : Dispatcher() {
				override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
					"/api/v1/chat/token" -> {
						tokenCalls++
						if (tokenStatus == 200) json("""{"code":"200","message":"ok","data":{"token":"chat-token-$tokenCalls","expiresAt":"2030-01-01T00:00:00Z","apiKey":"mpk_t","baseUrl":"https://chat.test","userId":"u-me"}}""")
						else MockResponse.Builder().code(tokenStatus).body("""{"code":"$tokenStatus","message":"Chat unavailable","data":null}""").build()
					}
					"/api/v1/chat/dm/u-budi" ->
						if (dmStatus == 200) json("""{"code":"200","message":"ok","data":{"channelId":"ch-9"}}""")
						else MockResponse.Builder().code(dmStatus).body("""{"code":"$dmStatus","message":"no","data":null}""").build()
					else -> MockResponse.Builder().code(404).build()
				}
			}
			start()
		}
		retrofit = Retrofit.Builder().baseUrl(server.url("/api/v1/")).addConverterFactory(GsonConverterFactory.create()).build()
	}

	@After
	fun tearDown() {
		scope.cancel()
		server.close()
	}

	private fun json(body: String) = MockResponse.Builder().code(200).body(body).build()

	// the network is real (MockWebServer), so these tests run in real time and wait for what they expect
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	private fun session() = MarpokatChatSession(retrofit, factory, pushTokens, scope)

	private fun test(body: suspend () -> Unit) = runBlocking { withTimeout(10_000) { body() } }

	private suspend fun until(what: String, condition: () -> Boolean) {
		repeat(400) { if (condition()) return; delay(25) }
		throw AssertionError("timed out: $what")
	}

	@Test
	fun startsOnceAndGetsReadyWithTheUserOfTheToken() = test {
		val session = session()
		session.start(); session.start()
		until("ready") { session.state.value is ChatSessionState.Ready }
		val ready = session.state.value as ChatSessionState.Ready
		assertThat(ready.userId).isEqualTo("u-me")
		assertThat(created).hasSize(1)
		assertThat(created[0].baseUrl).isEqualTo("https://chat.test")
		assertThat(tokenCalls).isEqualTo(1)
	}

	@Test
	fun registersThePushTokenOfThePhone() = test {
		pushTokens.onToken = { "fcm-1" }
		val session = session()
		session.start()
		until("push token registered") { "POST /v1/devices" in backend.calls }
		assertThat(backend.calls).contains("POST /v1/devices")
		assertThat(backend.bodies.last { it.contains("push_token") }).contains("fcm-1")
	}

	@Test
	fun aServiceThatIsOffMakesChatUnavailableAndStartTriesAgain() = test {
		tokenStatus = 503
		val session = session()
		session.start()
		until("unavailable") { session.state.value == ChatSessionState.Unavailable }
		assertThat(created).isEmpty()
		tokenStatus = 200
		session.start()
		until("ready") { session.state.value is ChatSessionState.Ready }
	}

	@Test
	fun aPushTokenThatRotatesIsSentAgain() = test {
		val session = session()
		session.start()
		until("ready") { session.state.value is ChatSessionState.Ready && "POST /v1/devices" in backend.calls }
		backend.bodies.clear()
		session.onPushToken("fcm-rotated")
		until("sent again") { backend.bodies.any { it.contains("push_token") } }
		assertThat(backend.bodies.single { it.contains("push_token") }).contains("fcm-rotated")
	}

	@Test
	fun aRotatedTokenBeforeTheSessionIsReadyIsIgnoredBecauseStartRegistersTheCurrentOne() = test {
		val session = session()
		session.onPushToken("early") // no client yet: nothing to send, and nothing may crash
		delay(100)
		assertThat(backend.calls).isEmpty()
	}

	@Test
	fun opensADirectMessageThroughTheBackend() = test {
		assertThat(session().openDirectMessage("u-budi")).isEqualTo("ch-9")
	}

	@Test
	fun aPersonThatCannotBeMessagedIsNotFoundAndAnOutageIsNot() = test {
		val session = session()
		dmStatus = 404
		val gone = runCatching { session.openDirectMessage("u-budi") }.exceptionOrNull() as ChatException
		assertThat(gone.notFound).isTrue()
		dmStatus = 503
		val down = runCatching { session.openDirectMessage("u-budi") }.exceptionOrNull() as ChatException
		assertThat(down.notFound).isFalse()
	}

	@Test
	fun signingOutRemovesThePushTokenStopsFollowingTheAppAndForgetsEverything() = test {
		pushTokens.onToken = { "fcm-1" }
		val session = session()
		session.start()
		until("ready") { session.state.value is ChatSessionState.Ready && "POST /v1/devices" in backend.calls }
		backend.calls.clear()
		session.signOut()
		assertThat(stopped).hasSize(1)
		assertThat(backend.calls).contains("DELETE /v1/devices/fcm-1")
		assertThat(session.state.value).isEqualTo(ChatSessionState.Idle)
		// a new user signing in starts from scratch
		session.start()
		until("ready again") { session.state.value is ChatSessionState.Ready }
		assertThat(created).hasSize(2)
	}

	@Test
	fun signingOutWithoutChatJustEndsIdle() = test {
		val session = session()
		session.signOut()
		assertThat(session.state.value).isEqualTo(ChatSessionState.Idle)
	}
}
