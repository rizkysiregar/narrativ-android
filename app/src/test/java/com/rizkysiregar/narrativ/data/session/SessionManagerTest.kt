package com.rizkysiregar.narrativ.data.session

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.JwtTokenHolder
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.testing.fakes.FakeAppAnalytics
import com.rizkysiregar.narrativ.testing.fakes.FakeCrashReporter
import com.rizkysiregar.narrativ.testing.preferencesFile
import com.rizkysiregar.narrativ.testing.testDataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class SessionManagerTest {

	@get:Rule
	val folder = TemporaryFolder()

	private val tokenHolder = JwtTokenHolder()
	private val analytics = FakeAppAnalytics()
	private val crashReporter = FakeCrashReporter()

	private class Env(val session: SessionManager, val dataStore: com.rizkysiregar.narrativ.data.cache.DataStoreManager)

	private suspend fun TestScope.env(): Env {
		val dataStore = testDataStoreManager(backgroundScope, folder.preferencesFile())
		dataStore.put(PrefKeys.token, "access")
		dataStore.put(PrefKeys.refreshToken, "refresh")
		dataStore.put(PrefKeys.isLoggedIn, true)
		tokenHolder.setToken("access")
		return Env(SessionManager(dataStore, tokenHolder, analytics, crashReporter, backgroundScope), dataStore)
	}

	@Test
	fun expiringTheSessionEndsItEverywhere() = runTest(UnconfinedTestDispatcher()) {
		val (session, dataStore) = env().let { it.session to it.dataStore }
		session.identify("user-1")

		session.expireSession("Sesi berakhir")

		assertThat(tokenHolder.getToken()).isNull()
		assertThat(dataStore.get(PrefKeys.token, "").first()).isEmpty()
		assertThat(dataStore.get(PrefKeys.refreshToken, "").first()).isEmpty()
		assertThat(dataStore.get(PrefKeys.isLoggedIn, false).first()).isFalse()
		assertThat(session.sessionExpiredMessage.value).isEqualTo("Sesi berakhir")
		assertThat(analytics.names()).containsExactly(AnalyticsEvents.SESSION_EXPIRED)
		assertThat(analytics.currentUserId).isNull()
		assertThat(crashReporter.currentUserId).isNull()
	}

	@Test
	fun theTokenIsDroppedBeforeAnythingIsAwaited() = runTest {
		// StandardTestDispatcher: the launched clean-up has not run yet when expireSession returns
		val session = env().session

		session.expireSession("x")

		assertThat(tokenHolder.getToken()).isNull()
		assertThat(session.sessionExpiredMessage.value).isNull()
	}

	@Test
	fun aSecondExpiryIsIgnoredAndKeepsTheFirstMessage() = runTest(UnconfinedTestDispatcher()) {
		val session = env().session

		session.expireSession("pertama")
		session.expireSession("kedua")

		assertThat(session.sessionExpiredMessage.value).isEqualTo("pertama")
		assertThat(analytics.names()).containsExactly(AnalyticsEvents.SESSION_EXPIRED)
	}

	@Test
	fun parallelRequestsThatAllGetA401ExpireTheSessionOnce() = runTest(UnconfinedTestDispatcher()) {
		val session = env().session
		val threads = 16
		val pool = Executors.newFixedThreadPool(threads)
		val start = CountDownLatch(1)
		val done = CountDownLatch(threads)

		repeat(threads) { i ->
			pool.execute {
				start.await()
				session.expireSession("sesi $i")
				done.countDown()
			}
		}
		start.countDown()
		done.await()
		pool.shutdown()

		assertThat(analytics.names().filter { it == AnalyticsEvents.SESSION_EXPIRED }).hasSize(1)
	}

	@Test
	fun signingInAgainReArmsTheExpiry() = runTest(UnconfinedTestDispatcher()) {
		val session = env().session
		session.expireSession("pertama")

		session.markAuthenticated()
		assertThat(session.sessionExpiredMessage.value).isNull()

		session.expireSession("kedua")
		assertThat(session.sessionExpiredMessage.value).isEqualTo("kedua")
		assertThat(analytics.names()).containsExactly(AnalyticsEvents.SESSION_EXPIRED, AnalyticsEvents.SESSION_EXPIRED)
	}

	@Test
	fun anExplicitLogoutClearsCredentialsWithoutAnExpiredMessage() = runTest(UnconfinedTestDispatcher()) {
		val (session, dataStore) = env().let { it.session to it.dataStore }
		session.identify("user-1")

		session.logout()

		assertThat(tokenHolder.getToken()).isNull()
		assertThat(dataStore.get(PrefKeys.token, "").first()).isEmpty()
		assertThat(dataStore.get(PrefKeys.refreshToken, "").first()).isEmpty()
		assertThat(session.sessionExpiredMessage.value).isNull()
		assertThat(analytics.names()).doesNotContain(AnalyticsEvents.SESSION_EXPIRED)
		assertThat(analytics.currentUserId).isNull()
	}

	@Test
	fun identifyLinksBothTelemetryServicesToTheOpaqueId() = runTest(UnconfinedTestDispatcher()) {
		val session = env().session

		session.identify("user-1")

		assertThat(analytics.currentUserId).isEqualTo("user-1")
		assertThat(crashReporter.currentUserId).isEqualTo("user-1")
	}
}
