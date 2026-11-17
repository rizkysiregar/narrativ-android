package com.rizkysiregar.narrativ.data.remote

/** Enqueues the background device sync. Safe to call often: a pending run is kept, not duplicated. */
interface DeviceSyncScheduler {
	/**
	 * @param force bypass the throttle and replace any pending sync; used right after login,
	 * where the device must be registered for the new session.
	 */
	fun schedule(force: Boolean = false)
}
