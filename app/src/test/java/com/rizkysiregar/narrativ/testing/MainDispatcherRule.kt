package com.rizkysiregar.narrativ.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Replaces Dispatchers.Main for tests, so a ViewModel's viewModelScope runs on [dispatcher].
 * Pass the same dispatcher to runTest(dispatcher) to share one virtual clock with the test body.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
	val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

	override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

	override fun finished(description: Description) = Dispatchers.resetMain()
}
