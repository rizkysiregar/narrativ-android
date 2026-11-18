package com.rizkysiregar.narrativ.ui.feature.home.settings

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.cache.JwtTokenHolder
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ForgotPasswordResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.narrativ.testing.MainDispatcherRule
import com.rizkysiregar.narrativ.testing.fakes.FakeAppStrings
import com.rizkysiregar.narrativ.testing.fakes.FakeGoogleIdTokenProvider
import com.rizkysiregar.narrativ.testing.fakes.FakeUserRepository
import com.rizkysiregar.narrativ.testing.fakes.failureOf
import com.rizkysiregar.narrativ.testing.fakes.successOf
import com.rizkysiregar.narrativ.testing.httpException
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
class ChangeEmailViewModelTest {

	@get:Rule
	val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

	@get:Rule
	val folder = TemporaryFolder()

	private val users = FakeUserRepository()
	private val google = FakeGoogleIdTokenProvider()
	private val strings = FakeAppStrings()
	private val tokenHolder = JwtTokenHolder()

	private class Env(val vm: ChangeEmailViewModel, val dataStore: DataStoreManager)

	private suspend fun TestScope.env(): Env {
		val dataStore = testDataStoreManager(backgroundScope, folder.preferencesFile())
		dataStore.put(PrefKeys.token, "old-access")
		dataStore.put(PrefKeys.refreshToken, "old-refresh")
		tokenHolder.setToken("old-access")
		return Env(ChangeEmailViewModel(users, google, dataStore, tokenHolder, strings), dataStore)
	}

	private val sent = ForgotPasswordResponse(otpSendToEmailAddress = "n****w@mail.com", expirationTimeInMinute = "5")
	private val session = LoginResponse(token = "new-access", refreshToken = "new-refresh", username = "rizky", userId = "u1")

	private suspend fun TestScope.codeSent(): Env {
		users.onSendEmailChangeOtp = { successOf(sent) }
		return env().also { it.vm.sendCode("  New@Mail.com ") }
	}

	@Test
	fun sendingTheCodeMovesOnToTheCodeStep() = runTest(mainRule.dispatcher) {
		val vm = codeSent().vm

		assertThat(users.emailChangeOtpRequests).containsExactly("new@mail.com")
		assertThat(vm.uiState.value.step).isEqualTo(ChangeEmailUiState.Step.ENTER_CODE)
		assertThat(vm.uiState.value.sentTo).isEqualTo("n****w@mail.com")
		assertThat(vm.uiState.value.isBusy).isFalse()
	}

	@Test
	fun aMalformedEmailIsNotSent() = runTest(mainRule.dispatcher) {
		val vm = env().vm

		vm.sendCode("not-an-email")

		assertThat(users.emailChangeOtpRequests).isEmpty()
		assertThat(vm.uiState.value.error).isEqualTo(strings.get(R.string.error_invalid_email))
	}

	@Test
	fun aRefusedAddressStaysOnTheEmailStep() = runTest(mainRule.dispatcher) {
		users.onSendEmailChangeOtp = { failureOf("This email is already registered.", httpException(400)) }
		val vm = env().vm

		vm.sendCode("taken@mail.com")

		assertThat(vm.uiState.value.step).isEqualTo(ChangeEmailUiState.Step.ENTER_EMAIL)
		// the global error dialog already showed the server's message
		assertThat(vm.uiState.value.error).isNull()
	}

	@Test
	fun confirmingStoresTheNewSessionSoThisDeviceStaysSignedIn() = runTest(mainRule.dispatcher) {
		val (vm, dataStore) = codeSent().let { it.vm to it.dataStore }
		users.onConfirmEmailChange = { successOf(session) }

		vm.confirmWithPassword("123456", "secret123")

		val request = users.emailChangeConfirmations.single()
		assertThat(request.email).isEqualTo("new@mail.com")
		assertThat(request.otp).isEqualTo("123456")
		assertThat(request.password).isEqualTo("secret123")
		assertThat(request.idToken).isNull()
		assertThat(dataStore.get(PrefKeys.token, "").first()).isEqualTo("new-access")
		assertThat(dataStore.get(PrefKeys.refreshToken, "").first()).isEqualTo("new-refresh")
		assertThat(tokenHolder.getToken()).isEqualTo("new-access")
		assertThat(vm.uiState.value.done).isTrue()
	}

	@Test
	fun aGoogleAccountConfirmsWithAFreshIdToken() = runTest(mainRule.dispatcher) {
		val vm = codeSent().vm
		users.onConfirmEmailChange = { successOf(session) }

		vm.confirmWithGoogle("123456")

		assertThat(users.emailChangeConfirmations.single().idToken).isEqualTo("google-id-token")
		assertThat(vm.uiState.value.done).isTrue()
	}

	@Test
	fun aWrongCodeKeepsTheOldSession() = runTest(mainRule.dispatcher) {
		val (vm, dataStore) = codeSent().let { it.vm to it.dataStore }
		users.onConfirmEmailChange = { failureOf("The OTP code you entered is incorrect.", httpException(400)) }

		vm.confirmWithPassword("000000", "secret123")

		assertThat(vm.uiState.value.done).isFalse()
		assertThat(dataStore.get(PrefKeys.refreshToken, "").first()).isEqualTo("old-refresh")
		assertThat(tokenHolder.getToken()).isEqualTo("old-access")
	}

	@Test
	fun anIncompleteCodeOrMissingPasswordIsNotSent() = runTest(mainRule.dispatcher) {
		val vm = codeSent().vm

		vm.confirmWithPassword("123", "secret123")
		assertThat(vm.uiState.value.error).isEqualTo(strings.get(R.string.error_otp_incomplete))

		vm.confirmWithPassword("123456", " ")
		assertThat(vm.uiState.value.error).isEqualTo(strings.get(R.string.error_password_required))

		assertThat(users.emailChangeConfirmations).isEmpty()
	}

	@Test
	fun resendAsksAgainForTheSameAddress() = runTest(mainRule.dispatcher) {
		val vm = codeSent().vm

		vm.resendCode()

		assertThat(users.emailChangeOtpRequests).containsExactly("new@mail.com", "new@mail.com").inOrder()
	}
}
