package com.rizkysiregar.feedzmore.data.remote.model.request.user

/** Proof of ownership: the password, or a fresh Google id token for an account that signs in with Google. */
data class DeleteAccountRequest(
	val password: String? = null,
	val idToken: String? = null
) {
	// never let a credential reach a log line
	override fun toString() = "DeleteAccountRequest(redacted)"
}
