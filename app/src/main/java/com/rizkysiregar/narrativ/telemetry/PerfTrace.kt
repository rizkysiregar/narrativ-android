package com.rizkysiregar.narrativ.telemetry

/** Names of the custom performance traces. */
object PerfTrace {
	const val VIDEO_COMPRESS = "video_compress"
	const val MEDIA_UPLOAD = "media_upload"
	const val POST_UPLOAD = "post_upload"
}

/**
 * Times a suspending block as a custom trace. The trace is stopped when the block fails or is cancelled as
 * well, with an "outcome" attribute telling the cases apart.
 */
interface PerfTracer {
	fun setCollectionEnabled(enabled: Boolean)

	suspend fun <T> measure(
		name: String,
		attributes: Map<String, String> = emptyMap(),
		block: suspend () -> T
	): T
}
