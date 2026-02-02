package com.rizkysiregar.feedzmore.data.remote.model.request.auth

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 02/03/26
 * Description: Software Engineer
 */
data class ResetPassVerifyOtpRequest(
	val email: String,
	val otp: String
)
