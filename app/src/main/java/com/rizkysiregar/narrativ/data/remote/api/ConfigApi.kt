package com.rizkysiregar.narrativ.data.remote.api

import com.rizkysiregar.narrativ.data.remote.model.response.BaseResponse
import com.rizkysiregar.narrativ.data.remote.model.response.ClientConfigResponse
import retrofit2.http.GET

/** The feature switches an admin flips in App management. Public: no login needed. */
interface ConfigApi {

	@GET("config/client")
	suspend fun clientConfig(): BaseResponse<ClientConfigResponse>
}
