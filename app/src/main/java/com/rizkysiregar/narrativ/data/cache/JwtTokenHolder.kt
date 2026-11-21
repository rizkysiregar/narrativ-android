package com.rizkysiregar.narrativ.data.cache

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 24/11/25
 */


@Singleton
class JwtTokenHolder @Inject constructor() {
	@Volatile
	private var token: String? = null

	fun setToken(value: String?) {
		token = value
	}

	fun getToken(): String? {
		return token
	}
}