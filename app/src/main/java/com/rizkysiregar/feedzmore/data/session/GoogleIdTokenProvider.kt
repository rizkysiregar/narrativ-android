package com.rizkysiregar.feedzmore.data.session

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rizkysiregar.feedzmore.constants.OAuthConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Asks the user to pick a Google account and returns a fresh id token for it: to sign in, and to prove
 * who they are before deleting the account.
 *
 * Throws [androidx.credentials.exceptions.GetCredentialException] when the user cancels or no account is available.
 */
@Singleton
class GoogleIdTokenProvider @Inject constructor(
	@ApplicationContext private val context: Context
) {
	suspend fun get(): String {
		val credentialManager = CredentialManager.create(context)

		val googleIdOption = GetGoogleIdOption.Builder()
			.setServerClientId(OAuthConfig.WEB_CLIENT_ID)
			.setFilterByAuthorizedAccounts(false)
			.setAutoSelectEnabled(false)
			.build()

		val request = GetCredentialRequest.Builder()
			.addCredentialOption(googleIdOption)
			.build()

		val credential = credentialManager.getCredential(context, request).credential

		// check the type before casting
		return when {
			credential is GoogleIdTokenCredential -> credential.idToken

			credential is CustomCredential &&
				credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ->
				GoogleIdTokenCredential.createFrom(credential.data).idToken

			else -> throw IllegalStateException("Unexpected credentials type: ${credential.type}")
		}
	}
}
