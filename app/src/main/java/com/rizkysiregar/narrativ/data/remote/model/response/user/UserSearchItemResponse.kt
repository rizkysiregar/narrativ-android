package com.rizkysiregar.narrativ.data.remote.model.response.user

import com.google.gson.annotations.SerializedName

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 */

data class UserSearchItemResponse(

	@field:SerializedName("id")
	val id: String,

	@field:SerializedName("username")
	val username: String,

	@field:SerializedName("fullName")
	val fullName: String? = null,

	@field:SerializedName("firstName")
	val firstName: String? = null,

	@field:SerializedName("lastName")
	val lastName: String? = null,

	@field:SerializedName("email")
	val email: String? = null,

	@field:SerializedName("phoneNumber")
	val phoneNumber: String? = null,

	@field:SerializedName("profileImageUrl")
	val profileImageUrl: String? = null
) {
	fun displayName(): String {
		val name = listOfNotNull(
			firstName?.trim()?.takeIf { it.isNotEmpty() },
			lastName?.trim()?.takeIf { it.isNotEmpty() }
		).joinToString(" ")

		return name.ifBlank { username }
	}
}
