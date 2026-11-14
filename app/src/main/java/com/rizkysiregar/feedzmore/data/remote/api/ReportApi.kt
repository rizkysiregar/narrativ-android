package com.rizkysiregar.feedzmore.data.remote.api

import com.rizkysiregar.feedzmore.data.remote.model.request.report.CreateReportRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ReportApi {
	@POST("reports")
	suspend fun createReport(@Body request: CreateReportRequest): BaseResponse<String>
}
