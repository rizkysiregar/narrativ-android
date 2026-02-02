package com.rizkysiregar.feedzmore.data.remote.model.response.auth

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 02/03/26
 * Description: Software Engineer
 */
data class ForgotPasswordResponse(
	val otpSendToEmailAddress: String,
	val expirationTimeInMinute: String
)
