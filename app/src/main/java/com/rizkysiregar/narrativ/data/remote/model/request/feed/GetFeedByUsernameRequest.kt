package com.rizkysiregar.narrativ.data.remote.model.request.feed

data class GetFeedByUsernameRequest(
    val page: Int,
    val size: Int,
    val username: String,
    // leave out caption only posts (grid view)
    val mediaOnly: Boolean = false
)