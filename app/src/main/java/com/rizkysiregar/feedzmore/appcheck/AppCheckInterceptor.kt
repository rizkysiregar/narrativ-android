package com.rizkysiregar.feedzmore.appcheck

import android.os.SystemClock
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.FirebaseAppCheck
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Adds the Firebase App Check token to every request to the backend, so the server can tell the
 * real app from a script. It only attaches the header: whether a missing or bad token is refused is the
 * backend's call. When no token can be had (offline, a device that fails the integrity check) the request
 * goes out without it instead of failing here, and further attempts pause for a while so a broken setup
 * does not slow down every call. The SDK caches the token, so most calls cost nothing.
 */
@Singleton
class AppCheckInterceptor @Inject constructor() : Interceptor {

	@Volatile
	private var pausedUntilMillis = 0L

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val token = currentToken() ?: return chain.proceed(request)
		return chain.proceed(request.newBuilder().header(HEADER, token).build())
	}

	private fun currentToken(): String? {
		if (SystemClock.elapsedRealtime() < pausedUntilMillis) return null
		return try {
			Tasks.await(FirebaseAppCheck.getInstance().getAppCheckToken(false), TOKEN_TIMEOUT_SECONDS, TimeUnit.SECONDS).token
		} catch (e: InterruptedException) {
			Thread.currentThread().interrupt()
			null
		} catch (e: Exception) {
			Log.w(TAG, "no App Check token, sending the request without one", e)
			pausedUntilMillis = SystemClock.elapsedRealtime() + PAUSE_MILLIS
			null
		}
	}

	private companion object {
		const val TAG = "AppCheck"
		const val HEADER = "X-Firebase-AppCheck"
		const val TOKEN_TIMEOUT_SECONDS = 10L
		const val PAUSE_MILLIS = 60_000L
	}
}
