package com.rizkysiregar.narrativ.domain.moderation

import androidx.annotation.StringRes
import com.rizkysiregar.narrativ.R

/** What a report can be about. [apiName] is what the server expects. */
enum class ReportTarget(val apiName: String) {
	POST("POST"),
	COMMENT("COMMENT"),
	USER("USER")
}

/** Why something is reported. [apiName] is what the server expects; [OTHER] needs a description. */
enum class ReportReason(val apiName: String, @StringRes val labelRes: Int) {
	SPAM("SPAM", R.string.report_reason_spam),
	HARASSMENT("HARASSMENT", R.string.report_reason_harassment),
	HATE_SPEECH("HATE_SPEECH", R.string.report_reason_hate_speech),
	SEXUAL_CONTENT("SEXUAL_CONTENT", R.string.report_reason_sexual),
	VIOLENCE("VIOLENCE", R.string.report_reason_violence),
	MISINFORMATION("MISINFORMATION", R.string.report_reason_misinformation),
	OTHER("OTHER", R.string.report_reason_other);

	val needsDescription: Boolean
		get() = this == OTHER
}

const val REPORT_DESCRIPTION_MAX_LENGTH = 500
