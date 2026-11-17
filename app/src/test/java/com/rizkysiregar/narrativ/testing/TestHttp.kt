package com.rizkysiregar.narrativ.testing

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

fun httpException(code: Int, body: String = """{"message":"error $code"}""") =
	HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))
