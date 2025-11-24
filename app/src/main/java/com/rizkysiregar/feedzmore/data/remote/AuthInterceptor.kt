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
		val requestBuilder = chain.request().newBuilder()

		if (!token.isNullOrEmpty()) {
			requestBuilder.addHeader("Authorization", "Bearer $token")
		}

		return chain.proceed(requestBuilder.build())
	}
}