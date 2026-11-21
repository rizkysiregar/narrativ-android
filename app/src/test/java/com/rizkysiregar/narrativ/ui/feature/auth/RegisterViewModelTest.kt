package com.rizkysiregar.narrativ.ui.feature.auth

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.auth.RegisterOtpResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.RegistrationTokenResponse
import com.rizkysiregar.narrativ.domain.auth.RegisterUseCase
import com.rizkysiregar.narrativ.domain.auth.SendRegisterOtpUseCase
import com.rizkysiregar.narrativ.domain.auth.VerifyRegisterOtpUseCase
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.testing.MainDispatcherRule
import com.rizkysiregar.narrativ.testing.MutableClock
import com.rizkysiregar.narrativ.testing.fakes.FakeAppAnalytics
import com.rizkysiregar.narrativ.testing.fakes.FakeAuthRepository
import com.rizkysiregar.narrativ.testing.fakes.failureOf
import com.rizkysiregar.narrativ.testing.fakes.successOf
import com.rizkysiregar.narrativ.ui.feature.auth.register.RegisterState
import com.rizkysiregar.narrativ.ui.feature.auth.register.RegisterStep
import com.rizkysiregar.narrativ.ui.feature.auth.register.RegisterViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

	@get:Rule
	val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

	private val auth = FakeAuthRepository()
	private val analytics = FakeAppAnalytics()
	private val clock = MutableClock()
	private val vm by lazy {
		RegisterViewModel(SendRegisterOtpUseCase(auth), VerifyRegisterOtpUseCase(auth), RegisterUseCase(auth), analytics, clock)
	}

	private val form = RegisterState(firstName = "Rizky", lastName = "S", username = "rizky", email = "r@mail.com", password = "secret123")

	private fun otpRequired(minutes: String = "5") = successOf(RegisterOtpResponse(true, "r***@mail.com", minutes))
	private fun otpNotRequired() = successOf(RegisterOtpResponse(false))

	@Test
	fun whereTheServerDoesNotVerifyEmailsTheFormRegistersDirectly() {
		auth.onSendRegisterOtp = { otpNotRequired() }
		auth.onRegister = { successOf("created") }

		vm.submit(form)

		val request = auth.registers.single()
		assertThat(request.registrationToken).isNull()
		assertThat(request.username).isEqualTo("rizky")
		assertThat(request.email).isEqualTo("r@mail.com")
		assertThat(vm.state.value).isEqualTo(ResultState.Success("created"))
		assertThat(vm.step.value).isEqualTo(RegisterStep.FORM)
	}

	@Test
	fun whereVerificationIsRequiredTheOtpStepOpensWithTheDeliveryInfo() {
		auth.onSendRegisterOtp = { otpRequired("5") }

		vm.submit(form)

		assertThat(vm.step.value).isEqualTo(RegisterStep.OTP_VERIFICATION)
		val delivery = vm.otpDelivery.value!!
		assertThat(delivery.otpSendToEmailAddress).isEqualTo("r***@mail.com")
		assertThat(delivery.expirationTimeInMinute).isEqualTo("5")
		assertThat(delivery.sentCount).isEqualTo(1)
		assertThat(auth.registers).isEmpty() // not registered before the OTP is verified
		assertThat(analytics.names()).containsExactly(AnalyticsEvents.REGISTER_OTP_SENT)
	}

	@Test
	fun resendingTheOtpRestartsTheCountdownByRaisingSentCount() {
		auth.onSendRegisterOtp = { otpRequired() }
		vm.submit(form)

		vm.resendOtp()

		assertThat(vm.otpDelivery.value!!.sentCount).isEqualTo(2)
		assertThat(auth.registerOtpEmails).containsExactly("r@mail.com", "r@mail.com")
	}

	@Test
	fun aVerifiedOtpRegistersWithTheRegistrationToken() {
		auth.onSendRegisterOtp = { otpRequired() }
		auth.onVerifyRegisterOtp = { successOf(RegistrationTokenResponse("reg-token")) }
		auth.onRegister = { successOf("created") }
		vm.submit(form)

		vm.verifyOtp("123456")

		assertThat(auth.verifyRegisterOtps.single().otp).isEqualTo("123456")
		assertThat(auth.verifyRegisterOtps.single().email).isEqualTo("r@mail.com")
		assertThat(auth.registers.single().registrationToken).isEqualTo("reg-token")
		assertThat(vm.state.value).isEqualTo(ResultState.Success("created"))
		assertThat(analytics.names()).containsAtLeast(AnalyticsEvents.REGISTER_OTP_VERIFIED, AnalyticsEvents.SIGN_UP)
	}

	@Test
	fun aWrongOtpStaysOnTheOtpStepWithTheError() {
		auth.onSendRegisterOtp = { otpRequired() }
		auth.onVerifyRegisterOtp = { failureOf("Kode salah") }
		vm.submit(form)

		vm.verifyOtp("000000")

		assertThat((vm.state.value as ResultState.Failure).message).isEqualTo("Kode salah")
		assertThat(vm.step.value).isEqualTo(RegisterStep.OTP_VERIFICATION)
		assertThat(auth.registers).isEmpty()
		assertThat(analytics.names()).contains(AnalyticsEvents.REGISTER_OTP_FAILED)
	}

	@Test
	fun aTakenUsernameSendsTheUserBackToTheFormWithTheError() {
		auth.onSendRegisterOtp = { otpRequired() }
		auth.onVerifyRegisterOtp = { successOf(RegistrationTokenResponse("reg-token")) }
		auth.onRegister = { failureOf("Username sudah dipakai") }
		vm.submit(form)

		vm.verifyOtp("123456")

		assertThat(vm.step.value).isEqualTo(RegisterStep.FORM)
		assertThat((vm.state.value as ResultState.Failure).message).isEqualTo("Username sudah dipakai")
		assertThat(analytics.names()).contains(AnalyticsEvents.SIGN_UP_FAILED)
	}

	/** The server refuses a new OTP inside its cooldown, so fixing a taken username must not ask for one. */
	@Test
	fun fixingTheUsernameWithinTheTokenLifetimeNeedsNoNewOtp() {
		auth.onSendRegisterOtp = { otpRequired() }
		auth.onVerifyRegisterOtp = { successOf(RegistrationTokenResponse("reg-token")) }
		var attempts = 0
		auth.onRegister = { if (attempts++ == 0) failureOf("Username sudah dipakai") else successOf("created") }
		vm.submit(form)
		vm.verifyOtp("123456")
		clock.advanceBy(Duration.ofMinutes(8))

		vm.submit(form.copy(username = "rizky2"))

		assertThat(auth.registerOtpEmails).hasSize(1) // no second OTP
		assertThat(auth.registers.last().username).isEqualTo("rizky2")
		assertThat(auth.registers.last().registrationToken).isEqualTo("reg-token")
		assertThat(vm.state.value).isEqualTo(ResultState.Success("created"))
	}

	@Test
	fun theTokenIsNotUsedPastNineMinutes() {
		auth.onSendRegisterOtp = { otpRequired() }
		auth.onVerifyRegisterOtp = { successOf(RegistrationTokenResponse("reg-token")) }
		auth.onRegister = { failureOf("Username sudah dipakai") }
		vm.submit(form)
		vm.verifyOtp("123456")
		clock.advanceBy(Duration.ofMinutes(9).plusSeconds(1)) // the server keeps it 10, the app stops at 9

		vm.submit(form.copy(username = "rizky2"))

		assertThat(auth.registerOtpEmails).hasSize(2)
		assertThat(vm.step.value).isEqualTo(RegisterStep.OTP_VERIFICATION)
	}

	@Test
	fun aDifferentEmailNeedsANewOtpEvenWithAFreshToken() {
		auth.onSendRegisterOtp = { otpRequired() }
		auth.onVerifyRegisterOtp = { successOf(RegistrationTokenResponse("reg-token")) }
		auth.onRegister = { failureOf("Username sudah dipakai") }
		vm.submit(form)
		vm.verifyOtp("123456")

		vm.submit(form.copy(email = "lain@mail.com"))

		assertThat(auth.registerOtpEmails).containsExactly("r@mail.com", "lain@mail.com").inOrder()
		assertThat(auth.registers).hasSize(1) // the token of the first email was not reused
	}

	@Test
	fun theFormIsKeptWhenGoingBackFromTheOtpStep() {
		auth.onSendRegisterOtp = { otpRequired() }
		vm.submit(form)

		vm.backToForm()

		assertThat(vm.step.value).isEqualTo(RegisterStep.FORM)
		assertThat(vm.state.value).isEqualTo(ResultState.Idle)
		assertThat(vm.form).isEqualTo(form)
	}
}
