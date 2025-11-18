package com.rizkysiregar.feedzmore.constants

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 16/11/25
 * Description: Software Engineer
 */
object OAuthConfig {
	const val CLIENT_ID = "feedzmore-android-local-cc"
	const val REDIRECT_URI = "com.rizkysiregar.feedzmore://oauth2redirect"
	const val ISSUER = "https://gyratory-damningly-alpha.ngrok-free.dev"
	val SCOPES = listOf("openid", "profile", "email")

	const val AUTH_URL = "$ISSUER/realms/master/protocol/openid-connect/auth"
	const val TOKEN_URL = "$ISSUER/realms/master/protocol/openid-connect/token"
	const val LOGOUT_URL = "$ISSUER/realms/master/protocol/openid-connect/logout"
}