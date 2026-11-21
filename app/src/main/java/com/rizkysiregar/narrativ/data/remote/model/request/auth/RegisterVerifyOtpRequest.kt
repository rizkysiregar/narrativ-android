package com.rizkysiregar.narrativ.data.remote.model.request.auth

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 */
data class RegisterVerifyOtpRequest(
	val email: String,
	val otp: String
)
