package com.rizkysiregar.feedzmore.data.remote.api

import com.rizkysiregar.feedzmore.data.remote.model.request.device.RegisterDeviceRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DeviceApi {
	// Response<Unit>: the result is intentionally not validated, so an HTTP error status
	// does not throw.
	@POST("devices")
	suspend fun registerDevice(@Body request: RegisterDeviceRequest): Response<Unit>
}
