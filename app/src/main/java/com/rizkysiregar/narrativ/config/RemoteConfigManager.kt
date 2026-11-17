package com.rizkysiregar.narrativ.config

import kotlinx.coroutines.flow.StateFlow

/**
 * The values the app reads from remote configuration. A failed fetch (offline, throttled) keeps the last
 * activated values, and on a first launch without any the in-app defaults apply, so an outage can never lock
 * anyone out of the app.
 */
interface RemoteConfigManager {
	val config: StateFlow<AppConfig>

	/** Fetches and activates new values. Never throws; a failure keeps the values already in effect. */
	suspend fun refresh()

	/** Completes once the first [refresh] has finished, whether it worked or not. */
	suspend fun awaitFirstRefresh()
}
