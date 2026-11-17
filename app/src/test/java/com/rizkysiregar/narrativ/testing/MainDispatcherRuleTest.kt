package com.rizkysiregar.narrativ.testing

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test

/** Guards the test toolchain itself: coroutines-test, Turbine and Truth are wired and Main is replaced. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {

	@get:Rule
	val mainRule = MainDispatcherRule()

	@Test
	fun mainDispatcherIsTheTestDispatcher() = runTest(mainRule.dispatcher) {
		val seen = withContext(Dispatchers.Main) { Thread.currentThread().name }

		assertThat(seen).isEqualTo(Thread.currentThread().name)
	}

	@Test
	fun stateFlowEmissionsCanBeAssertedWithTurbine() = runTest(mainRule.dispatcher) {
		val state = MutableStateFlow(0)

		state.test {
			assertThat(awaitItem()).isEqualTo(0)
			launch(Dispatchers.Main) { state.value = 1 }
			assertThat(awaitItem()).isEqualTo(1)
		}
	}
}
