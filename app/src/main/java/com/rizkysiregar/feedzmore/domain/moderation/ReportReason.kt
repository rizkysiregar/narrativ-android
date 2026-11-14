package com.rizkysiregar.feedzmore.domain.moderation

/** What a report can be about. [apiName] is what the server expects. */
enum class ReportTarget(val apiName: String) {
	POST("POST"),
	COMMENT("COMMENT"),
	USER("USER")
}

/** Why something is reported. [apiName] is what the server expects; [OTHER] needs a description. */
enum class ReportReason(val apiName: String, val label: String) {
	SPAM("SPAM", "Spam"),
	HARASSMENT("HARASSMENT", "Pelecehan atau perundungan"),
	HATE_SPEECH("HATE_SPEECH", "Ujaran kebencian"),
	SEXUAL_CONTENT("SEXUAL_CONTENT", "Konten seksual"),
	VIOLENCE("VIOLENCE", "Kekerasan"),
	MISINFORMATION("MISINFORMATION", "Informasi palsu"),
	OTHER("OTHER", "Lainnya");

	val needsDescription: Boolean
		get() = this == OTHER
}

const val REPORT_DESCRIPTION_MAX_LENGTH = 500
