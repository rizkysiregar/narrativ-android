package com.rizkysiregar.narrativ.data.remote.api

import com.rizkysiregar.narrativ.data.remote.model.request.device.DeactivateDeviceRequest
import com.rizkysiregar.narrativ.data.remote.model.request.device.RegisterDeviceRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DeviceApi {
	// Response<Unit>: the result is intentionally not validated, so an HTTP error status
	// does not throw.
	@POST("devices")
	suspend fun registerDevice(@Body request: RegisterDeviceRequest): Response<Unit>

	/** Stops push notifications to this device. Same lenient handling as [registerDevice]. */
	@POST("devices/deactivate")
	suspend fun deactivateDevice(@Body request: DeactivateDeviceRequest): Response<Unit>
}
