package com.rizkysiregar.narrativ.data.remote.model.response.auth

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 */

data class LoginResponse(
	val token: String,
	val refreshToken: String,
	val username: String,
	val userId: String,
	val role: String? = null
)
