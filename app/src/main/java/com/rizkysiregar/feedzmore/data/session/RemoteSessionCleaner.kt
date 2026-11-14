package com.rizkysiregar.feedzmore.data.session

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.api.AuthApi
import com.rizkysiregar.feedzmore.data.remote.api.DeviceApi
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.RefreshTokenRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.device.DeactivateDeviceRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ends the session on the server too, when the user logs out: the refresh token is revoked and this
 * device stops receiving push notifications for the account.
 *
 * Best effort. A user without a connection must still be able to log out, so every step swallows its
 * failure and the whole thing gives up after [TIMEOUT_MS]. The server drops a refresh token on its own
 * after 30 days, and a push token that moves to another account is reassigned when that account registers it.
 * Must run before the local session is cleared, since it needs the stored tokens.
 */
@Singleton
class RemoteSessionCleaner @Inject constructor(
	private val dataStore: DataStoreManager,
	retrofit: Retrofit
) {
	private val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
	private val deviceApi: DeviceApi by lazy { retrofit.create(DeviceApi::class.java) }

	suspend fun signOut() {
		withTimeoutOrNull(TIMEOUT_MS) {
			// the device first: it needs the access token, which the logout call does not
			attempt("deactivate device") {
				val pushToken = FirebaseMessaging.getInstance().token.await()
				deviceApi.deactivateDevice(DeactivateDeviceRequest(pushToken))
			}
			attempt("revoke refresh token") {
				val refreshToken = dataStore.get(PrefKeys.refreshToken, "").first()
				if (refreshToken.isNotEmpty()) authApi.logout(RefreshTokenRequest(refreshToken))
			}
		} ?: Log.w(TAG, "Remote sign out timed out")
		dropPushToken()
	}

	/** The next sign in gets a new push token, so nothing sent to the old one reaches this device again. */
	suspend fun dropPushToken() {
		attempt("delete push token") { FirebaseMessaging.getInstance().deleteToken().await() }
	}

	private suspend fun attempt(step: String, block: suspend () -> Unit) {
		try {
			block()
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			Log.w(TAG, "Remote sign out: $step failed", e)
		}
	}

	private companion object {
		const val TAG = "RemoteSessionCleaner"
		const val TIMEOUT_MS = 8_000L
	}
}
