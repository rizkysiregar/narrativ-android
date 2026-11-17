package com.rizkysiregar.narrativ.config

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.rizkysiregar.narrativ.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Fetches Firebase Remote Config and exposes it as [AppConfig]. It fails open: when a fetch
 * fails (offline, throttled) the last activated values stay in effect, and on a first launch without any
 * the in-app defaults apply, so a Remote Config outage can never lock anyone out of the app.
 */
@Singleton
class RemoteConfigManager @Inject constructor() {

	private val remoteConfig = FirebaseRemoteConfig.getInstance().apply {
		setConfigSettingsAsync(
			FirebaseRemoteConfigSettings.Builder()
				.setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else FETCH_INTERVAL_SECONDS)
				.setFetchTimeoutInSeconds(FETCH_TIMEOUT_SECONDS)
				.build()
		)
		setDefaultsAsync(
			mapOf(
				KEY_MIN_SUPPORTED_VERSION_CODE to 0L,
				KEY_UPDATE_MESSAGE to AppConfig.DEFAULT_UPDATE_MESSAGE
			)
		)
	}

	private val refreshMutex = Mutex()
	private val firstRefresh = CompletableDeferred<Unit>()

	private val _config = MutableStateFlow(read())
	val config: StateFlow<AppConfig> = _config.asStateFlow()

	/** Fetches and activates new values. Never throws; a failure keeps the values already in effect. */
	suspend fun refresh() {
		refreshMutex.withLock {
			try {
				val activated = remoteConfig.fetchAndActivate().await()
				_config.value = read()
				Log.d(TAG, "refresh: activated=$activated config=${_config.value}")
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				Log.w(TAG, "refresh: keeping the current config", e)
			} finally {
				firstRefresh.complete(Unit)
			}
		}
	}

	/** Completes once the first [refresh] has finished, whether it worked or not. */
	suspend fun awaitFirstRefresh() = firstRefresh.await()

	private fun read() = AppConfig(
		minSupportedVersionCode = remoteConfig.getLong(KEY_MIN_SUPPORTED_VERSION_CODE),
		updateMessage = remoteConfig.getString(KEY_UPDATE_MESSAGE).ifBlank { AppConfig.DEFAULT_UPDATE_MESSAGE }
	)

	private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
		addOnCompleteListener { task ->
			val error = task.exception
			when {
				error != null -> continuation.resumeWithException(error)
				task.isCanceled -> continuation.cancel()
				else -> continuation.resume(task.result)
			}
		}
	}

	private companion object {
		const val TAG = "RemoteConfig"
		const val KEY_MIN_SUPPORTED_VERSION_CODE = "min_supported_version_code"
		const val KEY_UPDATE_MESSAGE = "update_message"
		const val FETCH_INTERVAL_SECONDS = 3600L
		const val FETCH_TIMEOUT_SECONDS = 5L
	}
}
