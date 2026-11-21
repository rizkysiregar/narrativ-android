package com.rizkysiregar.narrativ.data.remote.model.request.auth

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 02/03/26
 */
data class ResetNewPasswordRequest(
	val email: String,
	val resetToken: String,
	val newPassword: String
)