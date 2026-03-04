package com.rizkysiregar.feedzmore.data.remote.model.request.auth

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 27/02/26
 * Description: Software Engineer
 */
data class RegisterRequest(
	val username: String,
	val password: String,
	val email: String,
	val firstName: String,
	val lastName: String,
	val phoneNumber: String
)