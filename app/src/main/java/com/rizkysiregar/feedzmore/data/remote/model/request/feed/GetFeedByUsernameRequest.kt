package com.rizkysiregar.feedzmore.data.remote.model.request.feed

data class GetFeedByUsernameRequest(
    val page: Int,
    val size: Int,
    val username: String
)