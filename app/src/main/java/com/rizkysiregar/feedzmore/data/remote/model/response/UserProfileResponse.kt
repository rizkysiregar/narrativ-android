package com.rizkysiregar.feedzmore.data.remote.model.response

data class UserProfileResponse(
	val firstName: String,
	val lastName: String,
	val phoneNumber: String,
	val fullName: String,
	val id: String,
	val profileImageUrl: String,
	val email: String,
	val username: String
)

