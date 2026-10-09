package com.rizkysiregar.feedzmore.util

import android.content.Context
import android.net.Uri
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.net.URLEncoder

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 05/12/25
 * Description: Software Engineer
 */


fun buildGetUrl(baseUrl: String, params: Map<String, String>): String {
	val builder = StringBuilder(baseUrl)

	if (!baseUrl.contains("?")) {
		builder.append("?")
	} else if (!baseUrl.endsWith("&")) {
		builder.append("&")
	}

	val query = params.map {
		"${it.key}=${URLEncoder.encode(it.value, "UTF-8")}"
	}.joinToString("&")

	builder.append(query)
	return builder.toString()
}

// convert URI to Request Body
fun uriToRequestBody(context: Context, uri: Uri, mimeType: String? = null): RequestBody {
	return object : RequestBody() {
		override fun contentType(): MediaType? {
			return (mimeType ?: "image/*").toMediaType()
		}

		override fun writeTo(sink: BufferedSink) {
			context.contentResolver.openInputStream(uri)?.use { inputStream ->
				sink.writeAll(inputStream.source())
			}
		}
	}
}