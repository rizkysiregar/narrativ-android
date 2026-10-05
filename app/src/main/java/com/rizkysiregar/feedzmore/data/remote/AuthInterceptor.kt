package com.rizkysiregar.feedzmore.data.remote

import com.rizkysiregar.feedzmore.data.cache.JwtTokenHolder
import com.rizkysiregar.feedzmore.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */

class AuthInterceptor @Inject constructor(
	private val tokenHolder: JwtTokenHolder,
	private val sessionManager: SessionManager
) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val token = tokenHolder.getToken()
		val request = chain.request()
		val requestBuilder = request.newBuilder()

		if (!token.isNullOrEmpty() && !isPublicAuthEndpoint(request.url.encodedPath)) {
			requestBuilder.header("Authorization", "Bearer $token")
		}

		val protectedRequest = !isPublicAuthEndpoint(request.url.encodedPath)
		val response = chain.proceed(requestBuilder.build())

		if (protectedRequest && response.code == HTTP_UNAUTHORIZED) {
			sessionManager.expireSession(extractErrorMessage(response))
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
	private fun extractErrorMessage(response: Response): String =
		runCatching {
			JSONObject(response.peekBody(MAX_ERROR_BODY_BYTES).string())
				.optString("message")
				.takeIf { it.isNotBlank() }
		}.getOrNull() ?: DEFAULT_SESSION_EXPIRED_MESSAGE

	private companion object {
		const val HTTP_UNAUTHORIZED = 401
		const val MAX_ERROR_BODY_BYTES = 64L * 1024L
		const val DEFAULT_SESSION_EXPIRED_MESSAGE =
			"Sesi Anda telah berakhir. Silakan masuk kembali."
	}
}
