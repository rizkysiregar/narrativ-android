package com.rizkysiregar.narrativ.data.model

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 17/11/25
 * Description: Software Engineer
 */
sealed class OAuth2LoginState {
	object Idle : OAuth2LoginState()
	object Loading : OAuth2LoginState()
	data class Success(
		val accessToken: String,
		val idToken: String,
		val refreshToken: String
	) : OAuth2LoginState()

	data class Error(val message: String) : OAuth2LoginState()
}