package com.rizkysiregar.narrativ.data.remote.model.response.user

import com.google.gson.annotations.SerializedName

data class FollowStatusResponse(

	@field:SerializedName("isFollowing")
	val isFollowing: Boolean,

	@field:SerializedName("followerCount")
	val followerCount: Long,

	@field:SerializedName("followingCount")
	val followingCount: Long
)

data class FollowUserResponse(

	@field:SerializedName("id")
	val id: String,

	@field:SerializedName("username")
	val username: String,

	@field:SerializedName("firstName")
	val firstName: String?,

	@field:SerializedName("lastName")
	val lastName: String?,

	@field:SerializedName("profileImage")
	val profileImage: String?
) {
	fun displayName(): String = listOfNotNull(firstName, lastName)
		.filter { it.isNotBlank() }
		.joinToString(" ")
		.ifEmpty { username }
}
