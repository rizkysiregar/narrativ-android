package com.rizkysiregar.narrativ.data.remote.model.request.auth

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 27/02/26
 */
data class RegisterRequest(
	val username: String,
	val password: String,
	val email: String,
	val firstName: String,
	val lastName: String,
	// proof of the verified email, issued after the registration OTP; null where the server does not verify emails
	val registrationToken: String? = null
)