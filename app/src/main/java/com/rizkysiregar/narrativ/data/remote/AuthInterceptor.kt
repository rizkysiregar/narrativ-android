package com.rizkysiregar.narrativ.data.remote

import com.rizkysiregar.narrativ.data.cache.JwtTokenHolder
import com.rizkysiregar.narrativ.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */

class AuthInterceptor @Inject constructor(
	private val tokenHolder: JwtTokenHolder,
	private val sessionManager: SessionManager,
	private val apiErrorManager: ApiErrorManager,
	private val tokenRefresher: TokenRefresher
) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val token = tokenHolder.getToken()
		val request = chain.request()
		val requestBuilder = request.newBuilder()

		if (!token.isNullOrEmpty() && !isPublicAuthEndpoint(request.url.encodedPath)) {
			requestBuilder.header("Authorization", "Bearer $token")
		}

		val protectedRequest = !isPublicAuthEndpoint(request.url.encodedPath)
		var response = try {
			chain.proceed(requestBuilder.build())
		} catch (e: IOException) {
			// A cancelled call (e.g. leaving a screen) is not a user-facing failure.
			if (!chain.call().isCanceled()) {
				apiErrorManager.report(ApiErrorEvent(null, ApiMessages.NETWORK_ERROR))
			}
			throw e
		}

		if (protectedRequest && response.code == HTTP_UNAUTHORIZED) {
			val message = extractErrorMessage(response)
			when (tokenRefresher.refresh(token)) {
				TokenRefresher.Result.REFRESHED -> {
					response.close()
					response = chain.proceed(
						request.newBuilder()
							.header("Authorization", "Bearer ${tokenHolder.getToken()}")
							.build()
					)
					if (response.code == HTTP_UNAUTHORIZED) {
						sessionManager.expireSession(extractErrorMessage(response))
					}
				}
				TokenRefresher.Result.REJECTED -> sessionManager.expireSession(message)
				// Network/server trouble while refreshing: keep the session, surface the 401.
				TokenRefresher.Result.FAILED -> Unit
			}
		}

		when {

			// 401 on auth endpoints (wrong password) is already shown inline by the auth
			// screens, and 404 is handled by callers on purpose (e.g. empty states).
			!response.isSuccessful &&
				response.code != HTTP_UNAUTHORIZED &&
				response.code != HTTP_NOT_FOUND ->
				apiErrorManager.report(
					ApiErrorEvent(response.code, extractErrorMessage(response, ApiMessages.GENERIC_ERROR))
				)
		}

		return response
	}

	/**
	 * Authentication endpoints must work even when a previously stored JWT has
	 * expired. Sending that stale token would make the backend reject the request
	 * in its JWT filter before the login or registration controller is reached.
	 */
	private fun isPublicAuthEndpoint(path: String): Boolean =
		path.contains("/auth/")

	/**
	 * peekBody reads a copy, so Retrofit can still read the original error body.
	 */
	private fun extractErrorMessage(
		response: Response,
		fallback: String = ApiMessages.SESSION_EXPIRED
	): String =
		runCatching {
			JSONObject(response.peekBody(MAX_ERROR_BODY_BYTES).string())
				.optString("message")
				.takeIf { it.isNotBlank() }
		}.getOrNull() ?: fallback

	private companion object {
		const val HTTP_UNAUTHORIZED = 401
		const val HTTP_NOT_FOUND = 404
		const val MAX_ERROR_BODY_BYTES = 64L * 1024L
	}
}
