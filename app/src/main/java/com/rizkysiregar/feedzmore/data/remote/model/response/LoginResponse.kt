package com.rizkysiregar.feedzmore.data.remote.model.response

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */

data class LoginResponse(
    val token: String,
    val username: String,
    val userId: String
)