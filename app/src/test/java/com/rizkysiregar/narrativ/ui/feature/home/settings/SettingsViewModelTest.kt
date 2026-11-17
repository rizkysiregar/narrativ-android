package com.rizkysiregar.narrativ.ui.feature.home.settings

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.JwtTokenHolder
import com.rizkysiregar.narrativ.data.session.SessionManager
import com.rizkysiregar.narrativ.telemetry.TelemetrySettings
import com.rizkysiregar.narrativ.testing.MainDispatcherRule
import com.rizkysiregar.narrativ.testing.fakes.FakeAppAnalytics
import com.rizkysiregar.narrativ.testing.fakes.FakeAppStrings
import com.rizkysiregar.narrativ.testing.fakes.FakeCrashReporter
import com.rizkysiregar.narrativ.testing.fakes.FakeDeviceSyncScheduler
import com.rizkysiregar.narrativ.testing.fakes.FakeGoogleIdTokenProvider
import com.rizkysiregar.narrativ.testing.fakes.FakePerfTracer
import com.rizkysiregar.narrativ.testing.fakes.FakeRemoteSessionCleaner
import com.rizkysiregar.narrativ.testing.fakes.FakeUserRepository
import com.rizkysiregar.narrativ.testing.fakes.failureOf
import com.rizkysiregar.narrativ.testing.fakes.successOf
import com.rizkysiregar.narrativ.testing.httpException
import com.rizkysiregar.narrativ.testing.preferencesFile
import com.rizkysiregar.narrativ.testing.testDataStoreManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

// Robolectric only for TelemetrySettings (SharedPreferences); a plain Application keeps Hilt and Firebase out.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class SettingsViewModelTest {

	@get:Rule
	val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

	@get:Rule
	val folder = TemporaryFolder()

	private val context: Context = ApplicationProvider.getApplicationContext()
	private val tokenHolder = JwtTokenHolder()
	private val cleaner = FakeRemoteSessionCleaner()
	private val users = FakeUserRepository()
	private val google = FakeGoogleIdTokenProvider()
	private val strings = FakeAppStrings()
	private val scheduler = FakeDeviceSyncScheduler()

	private class Env(val vm: SettingsViewModel, val dataStore: com.rizkysiregar.narrativ.data.cache.DataStoreManager)

	private suspend fun TestScope.env(): Env {
		val dataStore = testDataStoreManager(backgroundScope, folder.preferencesFile())
		dataStore.put(PrefKeys.token, "access")
		dataStore.put(PrefKeys.refreshToken, "refresh")
		tokenHolder.setToken("access")
		val analytics = FakeAppAnalytics()
		val crash = FakeCrashReporter()
		val session = SessionManager(dataStore, tokenHolder, analytics, crash, backgroundScope)
		val telemetry = TelemetrySettings(context, analytics, crash, FakePerfTracer())
		return Env(SettingsViewModel(session, cleaner, users, google, telemetry, strings, scheduler), dataStore)
	}

	// --- logout ---

	@Test
	fun logoutEndsTheSessionOnTheServerBeforeClearingItLocally() = runTest(mainRule.dispatcher) {
		val (vm, dataStore) = env().let { it.vm to it.dataStore }
		var tokenWhileSigningOut: String? = null
		cleaner.onSignOut = { tokenWhileSigningOut = tokenHolder.getToken() } // the cleaner needs the stored tokens
		var done = 0

		vm.logout { done++ }

		assertThat(tokenWhileSigningOut).isEqualTo("access")
		assertThat(tokenHolder.getToken()).isNull()
		assertThat(dataStore.get(PrefKeys.token, "").first()).isEmpty()
		assertThat(dataStore.get(PrefKeys.refreshToken, "").first()).isEmpty()
		assertThat(done).isEqualTo(1)
		assertThat(cleaner.calls).containsExactly("signOut")
	}

	@Test
	fun aSecondLogoutWhileOneIsRunningIsIgnored() = runTest(mainRule.dispatcher) {
		val vm = env().vm
		val gate = CompletableDeferred<Unit>()
		cleaner.onSignOut = { gate.await() }
		var done = 0

		vm.logout { done++ }
		assertThat(vm.uiState.value.isBusy).isTrue()
		vm.logout { done++ }
		gate.complete(Unit)

		assertThat(cleaner.calls).containsExactly("signOut")
		assertThat(done).isEqualTo(1)
	}

	// --- delete account ---

	@Test
	fun anEmptyPasswordIsRefusedBeforeAnyRequest() = runTest(mainRule.dispatcher) {
		val vm = env().vm

		vm.deleteAccountWithPassword("  ") {}

		assertThat(vm.uiState.value.deleteError).isEqualTo(strings.get(R.string.error_password_required))
		assertThat(users.deleteRequests).isEmpty()
		assertThat(vm.uiState.value.isBusy).isFalse()
	}

	@Test
	fun aDeletedAccountDropsThePushTokenAndSignsOutLocally() = runTest(mainRule.dispatcher) {
		val (vm, dataStore) = env().let { it.vm to it.dataStore }
		users.onDeleteAccount = { successOf("deleted") }
		var done = 0

		vm.deleteAccountWithPassword("secret") { done++ }

		assertThat(users.deleteRequests.single().password).isEqualTo("secret")
		assertThat(users.deleteRequests.single().idToken).isNull()
		assertThat(cleaner.calls).containsExactly("dropPushToken") // the server already revoked every session
		assertThat(tokenHolder.getToken()).isNull()
		assertThat(dataStore.get(PrefKeys.token, "").first()).isEmpty()
		assertThat(done).isEqualTo(1)
		assertThat(vm.uiState.value.isBusy).isFalse()
	}

	@Test
	fun aRefusedDeletionKeepsTheAccountAndTheSession() = runTest(mainRule.dispatcher) {
		val (vm, dataStore) = env().let { it.vm to it.dataStore }
		users.onDeleteAccount = { failureOf("Password salah", RuntimeException()) }
		var done = 0

		vm.deleteAccountWithPassword("wrong") { done++ }

		assertThat(vm.uiState.value.deleteError).isEqualTo("Password salah")
		assertThat(done).isEqualTo(0)
		assertThat(tokenHolder.getToken()).isEqualTo("access")
		assertThat(dataStore.get(PrefKeys.token, "").first()).isEqualTo("access")
		assertThat(cleaner.calls).isEmpty()
		assertThat(vm.uiState.value.isBusy).isFalse()
	}

	@Test
	fun httpAndNetworkFailuresAreNotRepeatedBecauseTheGlobalDialogShowedThem() = runTest(mainRule.dispatcher) {
		val vm = env().vm

		users.onDeleteAccount = { failureOf("Server error", httpException(500)) }
		vm.deleteAccountWithPassword("secret") {}
		assertThat(vm.uiState.value.deleteError).isNull()

		users.onDeleteAccount = { failureOf("Tidak ada koneksi", IOException()) }
		vm.deleteAccountWithPassword("secret") {}
		assertThat(vm.uiState.value.deleteError).isNull()
	}

	@Test
	fun aGoogleAccountDeletesWithTheIdTokenAsProof() = runTest(mainRule.dispatcher) {
		val vm = env().vm
		google.onGet = { "proof-token" }
		users.onDeleteAccount = { successOf("deleted") }
		var done = 0

		vm.deleteAccountWithGoogle { done++ }

		assertThat(users.deleteRequests.single().idToken).isEqualTo("proof-token")
		assertThat(users.deleteRequests.single().password).isNull()
		assertThat(done).isEqualTo(1)
	}

	@Test
	fun closingTheGoogleAccountPickerIsSilent() = runTest(mainRule.dispatcher) {
		val vm = env().vm
		google.onGet = { throw GetCredentialCancellationException("ditutup") }

		vm.deleteAccountWithGoogle {}

		assertThat(vm.uiState.value.deleteError).isNull()
		assertThat(users.deleteRequests).isEmpty()
		assertThat(vm.uiState.value.isBusy).isFalse()
	}

	@Test
	fun aGoogleVerificationErrorIsShown() = runTest(mainRule.dispatcher) {
		val vm = env().vm
		google.onGet = { throw GetCredentialUnknownException("gagal") }

		vm.deleteAccountWithGoogle {}

		assertThat(vm.uiState.value.deleteError).isEqualTo(strings.get(R.string.error_google_verify))
		assertThat(users.deleteRequests).isEmpty()
	}

	@Test
	fun aSecondDeleteWhileOneIsRunningIsIgnored() = runTest(mainRule.dispatcher) {
		val vm = env().vm
		val gate = CompletableDeferred<com.rizkysiregar.narrativ.data.remote.model.ResultState<String>>()
		users.onDeleteAccount = { kotlinx.coroutines.flow.flow { emit(gate.await()) } }

		vm.deleteAccountWithPassword("secret") {}
		vm.deleteAccountWithPassword("secret") {}

		assertThat(users.deleteRequests).hasSize(1)
		assertThat(vm.uiState.value.isBusy).isTrue()
	}

	@Test
	fun theDeleteErrorCanBeDismissed() = runTest(mainRule.dispatcher) {
		val vm = env().vm
		vm.deleteAccountWithPassword("") {}
		assertThat(vm.uiState.value.deleteError).isNotNull()

		vm.clearDeleteError()

		assertThat(vm.uiState.value.deleteError).isNull()
	}
}
