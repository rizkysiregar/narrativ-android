package com.rizkysiregar.narrativ.data.repository

/**
 * Sends this device's info (FCM token, OS, model, ...) to `POST /devices`.
 *
 * Runs from a background worker. The response is not validated: once the server answers
 * (whatever the status) the sync counts as done. Only failing to reach the server (or to get a
 * push token) is retried. A completed sync is remembered so the same user/token is not
 * re-sent within [SYNC_INTERVAL_HOURS].
 */
interface DeviceRegistrar {
	/**
	 * @param force skip the "already up to date" check, e.g. right after login.
	 * @return true when done (sent or not needed), false when the caller should retry
	 * because the request could not be made.
	 */
	suspend fun sync(force: Boolean = false): Boolean
}
