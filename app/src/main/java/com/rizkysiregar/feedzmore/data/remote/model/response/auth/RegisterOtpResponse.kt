package com.rizkysiregar.feedzmore.data.remote.model.response.auth

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Software Engineer
 */

/**
 * Answer of the registration OTP request. When [verificationRequired] is false the server does not verify emails
 * (development): no mail was sent and the app registers right away.
 */
data class RegisterOtpResponse(
	val verificationRequired: Boolean,
	val otpSendToEmailAddress: String? = null,
	val expirationTimeInMinute: String? = null
)
