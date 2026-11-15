package com.rizkysiregar.feedzmore.data.remote

import com.rizkysiregar.feedzmore.i18n.AppLocale
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Tells the API which language to word its messages in. */
class LanguageInterceptor @Inject constructor() : Interceptor {
	override fun intercept(chain: Interceptor.Chain): Response =
		chain.proceed(
			chain.request().newBuilder()
				.header("Accept-Language", AppLocale.current().tag)
				.build()
		)
}
