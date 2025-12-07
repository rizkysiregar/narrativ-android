package com.rizkysiregar.feedzmore.util

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