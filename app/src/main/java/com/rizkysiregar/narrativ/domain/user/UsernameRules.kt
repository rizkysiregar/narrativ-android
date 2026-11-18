package com.rizkysiregar.narrativ.domain.user

/**
 * What a username may look like. The backend refuses anything else (RegisterUserRequest), so the form checks it
 * before the OTP is sent instead of failing at the very last step.
 */
object UsernameRules {
	const val MIN_LENGTH = 3
	const val MAX_LENGTH = 16
	private val ALLOWED = Regex("^[A-Za-z0-9._]+$")

	fun isValid(username: String): Boolean =
		username.length in MIN_LENGTH..MAX_LENGTH && ALLOWED.matches(username)
}
