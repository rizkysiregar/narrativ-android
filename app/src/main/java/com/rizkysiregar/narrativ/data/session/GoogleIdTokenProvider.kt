package com.rizkysiregar.narrativ.data.session

/**
 * Asks the user to pick a Google account and returns a fresh id token for it: to sign in, and to prove
 * who they are before deleting the account.
 *
 * Throws [androidx.credentials.exceptions.GetCredentialException] when the user cancels or no account is available.
 */
interface GoogleIdTokenProvider {
	suspend fun get(): String
}
