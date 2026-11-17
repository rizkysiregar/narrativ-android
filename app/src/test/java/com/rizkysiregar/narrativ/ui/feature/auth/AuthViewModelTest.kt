package com.rizkysiregar.narrativ.ui.feature.auth

import androidx.credentials.exceptions.GetCredentialUnknownException
import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.JwtTokenHolder
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.session.SessionManager
import com.rizkysiregar.narrativ.domain.auth.GoogleLoginUseCase
import com.rizkysiregar.narrativ.domain.auth.LoginUseCase
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.testing.MainDispatcherRule
import com.rizkysiregar.narrativ.testing.aLoginResponse
import com.rizkysiregar.narrativ.testing.fakes.FakeAppAnalytics
import com.rizkysiregar.narrativ.testing.fakes.FakeAppStrings
import com.rizkysiregar.narrativ.testing.fakes.FakeAuthRepository
import com.rizkysiregar.narrativ.testing.fakes.FakeCrashReporter
import com.rizkysiregar.narrativ.testing.fakes.FakeDeviceSyncScheduler
import com.rizkysiregar.narrativ.testing.fakes.FakeGoogleIdTokenProvider
import com.rizkysiregar.narrativ.testing.fakes.failureOf
import com.rizkysiregar.narrativ.testing.fakes.successOf
import com.rizkysiregar.narrativ.testing.preferencesFile
import com.rizkysiregar.narrativ.testing.testDataStoreManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

	@get:Rule
	val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

	@get:Rule
	val folder = TemporaryFolder()

	private val auth = FakeAuthRepository()
	private val tokenHolder = JwtTokenHolder()
	private val analytics = FakeAppAnalytics()
	private val scheduler = FakeDeviceSyncScheduler()
	private val google = FakeGoogleIdTokenProvider()
	private val strings = FakeAppStrings()

	private class Env(
		val vm: AuthViewModel,
		val dataStore: com.rizkysiregar.narrativ.data.cache.DataStoreManager,
		val session: SessionManager
	)

	private fun TestScope.env(): Env {
		val dataStore = testDataStoreManager(backgroundScope, folder.preferencesFile())
		val session = SessionManager(dataStore, tokenHolder, analytics, FakeCrashReporter(), backgroundScope)
		val vm = AuthViewModel(
			LoginUseCase(auth), GoogleLoginUseCase(auth), dataStore, tokenHolder, session, scheduler, google, analytics, strings
		)
		return Env(vm, dataStore, session)
	}

	@Test
	fun aSuccessfulLoginStartsAFullSession() = runTest(mainRule.dispatcher) {
		val (vm, dataStore, session) = env().let { Triple(it.vm, it.dataStore, it.session) }
		val response = aLoginResponse(token = "t1", refreshToken = "r1", username = "rizky", userId = "u1")
		auth.onLogin = { successOf(response) }
		session.expireSession("sesi lama") // the user got logged out earlier

		vm.login("rizky", "secret")

		assertThat(vm.loginState.value).isEqualTo(ResultState.Success(response))
		assertThat(dataStore.get(PrefKeys.token, "").first()).isEqualTo("t1")
		assertThat(dataStore.get(PrefKeys.refreshToken, "").first()).isEqualTo("r1")
		assertThat(dataStore.get(PrefKeys.username, "").first()).isEqualTo("rizky")
		assertThat(dataStore.get(PrefKeys.userId, "").first()).isEqualTo("u1")
		assertThat(dataStore.get(PrefKeys.isLoggedIn, false).first()).isTrue()
		assertThat(tokenHolder.getToken()).isEqualTo("t1")
		assertThat(session.sessionExpiredMessage.value).isNull() // the old expiry notice is gone
		assertThat(analytics.currentUserId).isEqualTo("u1")
		assertThat(scheduler.scheduled).containsExactly(true) // forced, the device must register for this session
		assertThat(analytics.events.last().name).isEqualTo(AnalyticsEvents.LOGIN)
		assertThat(analytics.events.last().params[AnalyticsEvents.Param.METHOD]).isEqualTo("password")
		assertThat(auth.logins.single().username).isEqualTo("rizky")
	}

	@Test
	fun aFailedLoginStoresNothing() = runTest(mainRule.dispatcher) {
		val (vm, dataStore) = env().let { it.vm to it.dataStore }
		auth.onLogin = { failureOf("Password salah") }

		vm.login("rizky", "wrong")

		assertThat((vm.loginState.value as ResultState.Failure).message).isEqualTo("Password salah")
		assertThat(dataStore.get(PrefKeys.token, "").first()).isEmpty()
		assertThat(dataStore.get(PrefKeys.isLoggedIn, false).first()).isFalse()
		assertThat(tokenHolder.getToken()).isNull()
		assertThat(scheduler.scheduled).isEmpty()
		assertThat(analytics.names()).containsExactly(AnalyticsEvents.LOGIN_FAILED)
	}

	@Test
	fun theLoginStateIsLoadingAsSoonAsLoginStarts() = runTest(mainRule.dispatcher) {
		val vm = env().vm
		val never = kotlinx.coroutines.flow.MutableSharedFlow<ResultState<com.rizkysiregar.narrativ.data.remote.model.response.auth.LoginResponse>>()
		auth.onLogin = { never }

		vm.login("a", "b")

		assertThat(vm.loginState.value).isEqualTo(ResultState.Loading)
	}

	@Test
	fun resettingTheStatesGoesBackToIdle() = runTest(mainRule.dispatcher) {
		val vm = env().vm

		vm.resetLoginState()
		vm.resetGoogleLoginState()

		assertThat(vm.loginState.value).isEqualTo(ResultState.Idle)
		assertThat(vm.googleLoginState.value).isEqualTo(ResultState.Idle)
	}

	@Test
	fun googleSignInSendsTheIdTokenAndStartsASession() = runTest(mainRule.dispatcher) {
		val (vm, dataStore) = env().let { it.vm to it.dataStore }
		google.onGet = { "id-token-xyz" }
		auth.onGoogleLogin = { successOf(aLoginResponse(token = "g1", userId = "gu")) }

		vm.signInWithGoogle()

		assertThat(auth.googleLogins.single().idToken).isEqualTo("id-token-xyz")
		assertThat(dataStore.get(PrefKeys.token, "").first()).isEqualTo("g1")
		assertThat(analytics.events.last().params[AnalyticsEvents.Param.METHOD]).isEqualTo("google")
	}

	@Test
	fun cancellingTheGoogleAccountPickerShowsItsMessage() = runTest(mainRule.dispatcher) {
		val vm = env().vm
		google.onGet = { throw GetCredentialUnknownException("Pengguna membatalkan") }

		vm.signInWithGoogle()

		assertThat((vm.googleLoginState.value as ResultState.Failure).message).isEqualTo("Pengguna membatalkan")
		assertThat(auth.googleLogins).isEmpty()
	}

	@Test
	fun aGoogleErrorWithoutAMessageUsesTheGenericText() = runTest(mainRule.dispatcher) {
		val vm = env().vm
		google.onGet = { throw IllegalStateException() }

		vm.signInWithGoogle()

		assertThat((vm.googleLoginState.value as ResultState.Failure).message).isEqualTo(strings.get(R.string.error_generic))
	}

	@Test
	fun aGoogleLoginRejectedByTheServerCountsAsAFailedLogin() = runTest(mainRule.dispatcher) {
		val vm = env().vm
		auth.onGoogleLogin = { failureOf("Akun tidak dikenal") }

		vm.signInWithGoogle()

		assertThat((vm.googleLoginState.value as ResultState.Failure).message).isEqualTo("Akun tidak dikenal")
		assertThat(analytics.names()).containsExactly(AnalyticsEvents.LOGIN_FAILED)
	}
}
