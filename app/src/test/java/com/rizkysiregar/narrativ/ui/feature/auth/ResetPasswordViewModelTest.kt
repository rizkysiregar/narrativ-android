package com.rizkysiregar.narrativ.ui.feature.auth

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.auth.ResetNewPasswordRequest
import com.rizkysiregar.narrativ.data.remote.model.request.auth.ResetPassVerifyOtpRequest
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ForgotPasswordResponse
import com.rizkysiregar.narrativ.data.remote.model.response.auth.ResetPassVerifyOtpResponse
import com.rizkysiregar.narrativ.domain.auth.ResetNewPasswordUseCase
import com.rizkysiregar.narrativ.domain.auth.SendResetOtpUseCase
import com.rizkysiregar.narrativ.domain.auth.VerifyOtpResetPasswordUseCase
import com.rizkysiregar.narrativ.testing.MainDispatcherRule
import com.rizkysiregar.narrativ.testing.fakes.FakeAuthRepository
import com.rizkysiregar.narrativ.testing.fakes.failureOf
import com.rizkysiregar.narrativ.testing.fakes.successOf
import com.rizkysiregar.narrativ.ui.feature.auth.reset.ResetPasswordStep
import com.rizkysiregar.narrativ.ui.feature.auth.reset.ResetPasswordViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResetPasswordViewModelTest {

	@get:Rule
	val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

	private val auth = FakeAuthRepository()
	private val vm by lazy {
		ResetPasswordViewModel(SendResetOtpUseCase(auth), VerifyOtpResetPasswordUseCase(auth), ResetNewPasswordUseCase(auth))
	}

	@Test
	fun theFlowStartsOnTheEmailStep() {
		assertThat(vm.currentStep.value).isEqualTo(ResetPasswordStep.EMAIL_ENTRY)
	}

	@Test
	fun aSentOtpMovesToVerificationAndKeepsTheDelivery() {
		auth.onForgotPassword = { successOf(ForgotPasswordResponse("r***@mail.com", "5")) }

		vm.sendOtp("r@mail.com")

		assertThat(vm.currentStep.value).isEqualTo(ResetPasswordStep.OTP_VERIFICATION)
		assertThat(vm.otpDelivery.value!!.expirationTimeInMinute).isEqualTo("5")
		assertThat(vm.getSavedEmail()).isEqualTo("r@mail.com")
	}

	@Test
	fun aFailedSendStaysOnTheEmailStep() {
		auth.onForgotPassword = { failureOf("Email tidak terdaftar") }

		vm.sendOtp("x@mail.com")

		assertThat(vm.currentStep.value).isEqualTo(ResetPasswordStep.EMAIL_ENTRY)
		assertThat((vm.resetState.value as ResultState.Failure).message).isEqualTo("Email tidak terdaftar")
		assertThat(vm.otpDelivery.value).isNull()
	}

	@Test
	fun aVerifiedOtpKeepsTheResetTokenAndOpensTheNewPasswordStep() {
		auth.onVerifyOtp = { successOf(ResetPassVerifyOtpResponse("reset-token")) }

		vm.verifyOtp(ResetPassVerifyOtpRequest("r@mail.com", "123456"))

		assertThat(vm.currentStep.value).isEqualTo(ResetPasswordStep.NEW_PASSWORD_INPUT)
		assertThat(vm.getSavedResetToken()).isEqualTo("reset-token")
	}

	@Test
	fun aWrongOtpStaysOnTheVerificationStepAndSavesNoToken() {
		auth.onForgotPassword = { successOf(ForgotPasswordResponse("r***@mail.com", "5")) }
		auth.onVerifyOtp = { failureOf("Kode salah") }
		vm.sendOtp("r@mail.com")

		vm.verifyOtp(ResetPassVerifyOtpRequest("r@mail.com", "000000"))

		assertThat(vm.currentStep.value).isEqualTo(ResetPasswordStep.OTP_VERIFICATION)
		assertThat(vm.getSavedResetToken()).isEmpty()
	}

	@Test
	fun theNewPasswordFinishesTheFlowOnlyOnSuccess() {
		auth.onResetNewPassword = { failureOf("Password terlalu lemah") }
		vm.resetNewPassword(ResetNewPasswordRequest("r@mail.com", "reset-token", "123"))
		assertThat(vm.currentStep.value).isNotEqualTo(ResetPasswordStep.FINISH)

		auth.onResetNewPassword = { successOf("ok") }
		vm.resetNewPassword(ResetNewPasswordRequest("r@mail.com", "reset-token", "Kuat123!"))

		assertThat(vm.currentStep.value).isEqualTo(ResetPasswordStep.FINISH)
	}

	@Test
	fun resettingTheStateGoesBackToIdle() {
		auth.onForgotPassword = { failureOf("x") }
		vm.sendOtp("x@mail.com")

		vm.resetStateToIdle()

		assertThat(vm.resetState.value).isEqualTo(ResultState.Idle)
	}
}
