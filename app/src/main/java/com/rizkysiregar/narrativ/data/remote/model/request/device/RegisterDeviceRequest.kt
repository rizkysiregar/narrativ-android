package com.rizkysiregar.narrativ.data.remote.model.request.device

data class RegisterDeviceRequest(
	val pushToken: String,
	val platform: String,
	val deviceId: String?,
	val appVersion: String?,
	val osVersion: String?,
	val deviceModel: String?,
	val locale: String?,
	val timezone: String?
)
