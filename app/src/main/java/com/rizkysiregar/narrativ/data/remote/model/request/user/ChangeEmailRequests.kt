package com.rizkysiregar.narrativ.data.remote.model.request.user

/** Step 1 of an email change: the new address the OTP goes to. */
data class ChangeEmailOtpRequest(
	val email: String
)

/**
 * Step 2: the OTP sent to the new address plus proof of ownership, the password or a fresh Google id token for an
 * account that signs in with Google.
 */
data class ConfirmEmailChangeRequest(
	val email: String,
	val otp: String,
	val password: String? = null,
	val idToken: String? = null
) {
	// never let a credential reach a log line
	override fun toString() = "ConfirmEmailChangeRequest(redacted)"
}
