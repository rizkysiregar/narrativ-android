package com.rizkysiregar.narrativ.data.remote

import android.content.Context
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.i18n.AppStrings

/**
 * User-facing fallback messages shared by the network interceptor and the repository layer. They are read when
 * needed, in the current language; [init] gives them an application context, which top-level helpers do not have.
 */
object ApiMessages {
	private var appContext: Context? = null

	fun init(context: Context) {
		appContext = context.applicationContext
	}

	val NETWORK_ERROR: String get() = text(R.string.api_network_error, "Cannot reach the server. Check your internet connection.")
	val GENERIC_ERROR: String get() = text(R.string.api_generic_error, "Something went wrong. Please try again later.")
	val SESSION_EXPIRED: String get() = text(R.string.api_session_expired, "Your session has ended. Please sign in again.")

	private fun text(id: Int, fallback: String): String =
		appContext?.let { AppStrings.localized(it).getString(id) } ?: fallback
}
