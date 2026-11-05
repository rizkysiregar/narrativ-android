package com.rizkysiregar.feedzmore.data.remote

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global channel for API failures that should be surfaced to the user as an alert,
 * regardless of which screen triggered the request.
 *
 * [code] is the HTTP status, or null for network failures (timeout, no connection).
 */
data class ApiErrorEvent(val code: Int?, val message: String)

@Singleton
class ApiErrorManager @Inject constructor() {
	private val _errors = MutableSharedFlow<ApiErrorEvent>(
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)
	val errors: SharedFlow<ApiErrorEvent> = _errors.asSharedFlow()

	fun report(event: ApiErrorEvent) {
		_errors.tryEmit(event)
	}
}
