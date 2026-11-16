package com.rizkysiregar.narrativ.data.remote.model.request.report

/** [targetType] and [reason] are the names the server expects, see [ReportTarget] and [ReportReason]. */
data class CreateReportRequest(
	val targetType: String,
	val targetId: String,
	val reason: String,
	val description: String? = null
)
