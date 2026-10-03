package com.rizkysiregar.feedzmore.data.remote

import com.rizkysiregar.feedzmore.data.cache.JwtTokenHolder
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */

class AuthInterceptor @Inject constructor(
	val tokenHolder: JwtTokenHolder
) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val token = tokenHolder.getToken()
		val request = chain.request()
		val requestBuilder = request.newBuilder()

		if (!token.isNullOrEmpty() && !isPublicAuthEndpoint(request.url.encodedPath)) {
			requestBuilder.header("Authorization", "Bearer $token")
		}

		return chain.proceed(requestBuilder.build())
	}

	/**
	 * Authentication endpoints must work even when a previously stored JWT has
	 * expired. Sending that stale token would make the backend reject the request
	 * in its JWT filter before the login or registration controller is reached.
	 */
	private fun isPublicAuthEndpoint(path: String): Boolean =
		path.contains("/auth/")
}
