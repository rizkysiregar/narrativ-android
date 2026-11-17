package com.rizkysiregar.narrativ.data.session

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.remote.api.AuthApi
import com.rizkysiregar.narrativ.data.remote.api.DeviceApi
import com.rizkysiregar.narrativ.data.remote.model.request.auth.RefreshTokenRequest
import com.rizkysiregar.narrativ.data.remote.model.request.device.DeactivateDeviceRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/** [RemoteSessionCleaner] on the API and Firebase Messaging. */
@Singleton
class FirebaseRemoteSessionCleaner @Inject constructor(
	private val dataStore: DataStoreManager,
	retrofit: Retrofit
) : RemoteSessionCleaner {
	private val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
	private val deviceApi: DeviceApi by lazy { retrofit.create(DeviceApi::class.java) }

	override suspend fun signOut() {
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

	override suspend fun dropPushToken() {
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
