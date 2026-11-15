package com.rizkysiregar.feedzmore.telemetry

import com.google.firebase.perf.FirebasePerformance

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Times a suspending block as a Firebase Performance custom trace. The trace is stopped when
 * the block fails or is cancelled as well, with an "outcome" attribute telling the cases apart.
 */
object PerfTrace {
	const val VIDEO_COMPRESS = "video_compress"
	const val MEDIA_UPLOAD = "media_upload"
	const val POST_UPLOAD = "post_upload"

	fun setCollectionEnabled(enabled: Boolean) {
		FirebasePerformance.getInstance().isPerformanceCollectionEnabled = enabled
	}

	suspend fun <T> measure(
		name: String,
		attributes: Map<String, String> = emptyMap(),
		block: suspend () -> T
	): T {
		val trace = FirebasePerformance.getInstance().newTrace(name)
		attributes.forEach { (key, value) -> trace.putAttribute(key, value) }
		trace.start()
		var outcome = "failed"
		try {
			return block().also { outcome = "success" }
		} catch (e: kotlinx.coroutines.CancellationException) {
			outcome = "cancelled"
			throw e
		} finally {
			trace.putAttribute("outcome", outcome)
			trace.stop()
		}
	}
}
