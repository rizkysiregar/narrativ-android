package com.rizkysiregar.narrativ.data.session

/**
 * Ends the session on the server too, when the user logs out: the refresh token is revoked and this
 * device stops receiving push notifications for the account.
 *
 * Best effort. A user without a connection must still be able to log out, so every step swallows its
 * failure and the whole thing gives up after 8 seconds. The server drops a refresh token on its own
 * after 30 days, and a push token that moves to another account is reassigned when that account registers it.
 * Must run before the local session is cleared, since it needs the stored tokens.
 */
interface RemoteSessionCleaner {
	suspend fun signOut()

	/** The next sign in gets a new push token, so nothing sent to the old one reaches this device again. */
	suspend fun dropPushToken()
}
