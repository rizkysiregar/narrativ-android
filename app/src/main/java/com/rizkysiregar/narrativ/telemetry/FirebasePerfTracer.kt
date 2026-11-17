package com.rizkysiregar.narrativ.telemetry

import com.google.firebase.perf.FirebasePerformance
import javax.inject.Inject
import javax.inject.Singleton

/** [PerfTracer] on Firebase Performance. */
@Singleton
class FirebasePerfTracer @Inject constructor() : PerfTracer {

	override fun setCollectionEnabled(enabled: Boolean) {
		FirebasePerformance.getInstance().isPerformanceCollectionEnabled = enabled
	}

	override suspend fun <T> measure(
		name: String,
		attributes: Map<String, String>,
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
