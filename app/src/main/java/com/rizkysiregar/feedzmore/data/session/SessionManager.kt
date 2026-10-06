package com.rizkysiregar.feedzmore.data.session

import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.cache.JwtTokenHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the local authentication session and exposes global session-expiry state.
 *
 * Networking code can invalidate a session without knowing anything about the UI,
 * while the UI can observe [sessionExpiredMessage] and perform navigation on the main
 * thread.
 */
@Singleton
class SessionManager @Inject constructor(
	private val dataStore: DataStoreManager,
	private val tokenHolder: JwtTokenHolder
) {
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val invalidationInProgress = AtomicBoolean(false)
	private val _sessionExpiredMessage = MutableStateFlow<String?>(null)
	val sessionExpiredMessage: StateFlow<String?> = _sessionExpiredMessage.asStateFlow()

	/** Called by the network layer when a protected endpoint returns HTTP 401. */
	fun expireSession(message: String) {
		if (!invalidationInProgress.compareAndSet(false, true)) return

		// Stop subsequent parallel requests from sending an already rejected token.
		tokenHolder.setToken(null)

		scope.launch {
			dataStore.clearAll()
			_sessionExpiredMessage.value = message
		}
	}

	/** Clears local credentials for an explicit user logout. */
	fun logout() {
		tokenHolder.setToken(null)
		scope.launch {
			dataStore.clearAll()
		}
	}

	/** Resets expiry state after a new successful login. */
	fun markAuthenticated() {
		invalidationInProgress.set(false)
		_sessionExpiredMessage.value = null
	}
}
