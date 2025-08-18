package com.rizkysiregar.feedzmore_android_native.data.remote.model

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */

data class BaseResponse<T>(
    val code: String,
    val message: String,
    val data: T?
)