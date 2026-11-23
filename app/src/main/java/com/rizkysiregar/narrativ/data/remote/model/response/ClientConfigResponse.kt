package com.rizkysiregar.narrativ.data.remote.model.response

/** Which features the app shows. A missing value is off. */
data class ClientConfigResponse(val features: Features? = null) {
	data class Features(val chat: Boolean = false)
}
