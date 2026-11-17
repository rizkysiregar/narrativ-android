package com.rizkysiregar.narrativ.testing.fakes

import com.rizkysiregar.narrativ.telemetry.AppAnalytics
import com.rizkysiregar.narrativ.telemetry.CrashReporter
import com.rizkysiregar.narrativ.telemetry.PerfTracer
import java.util.concurrent.CopyOnWriteArrayList

/** Records what was logged so a test can assert on it. Safe to call from several threads. */
class FakeAppAnalytics : AppAnalytics {
	data class Event(val name: String, val params: Map<String, Any>)

	val events = CopyOnWriteArrayList<Event>()
	val screens = CopyOnWriteArrayList<String>()

	@Volatile
	var currentUserId: String? = null

	@Volatile
	var collectionEnabled: Boolean? = null

	fun names(): List<String> = events.map { it.name }

	override fun log(event: String, vararg params: Pair<String, Any>) {
		events += Event(event, params.toMap())
	}

	override fun logScreen(route: String) {
		screens += route
	}

	override fun setCollectionEnabled(enabled: Boolean) {
		collectionEnabled = enabled
	}

	override fun setUserId(userId: String?) {
		this.currentUserId = userId
	}
}

class FakeCrashReporter : CrashReporter {
	data class NonFatal(val throwable: Throwable, val context: String, val keys: Map<String, String>)

	val nonFatals = CopyOnWriteArrayList<NonFatal>()
	val breadcrumbs = CopyOnWriteArrayList<String>()

	@Volatile
	var currentUserId: String? = null

	override fun recordNonFatal(throwable: Throwable, context: String, vararg keys: Pair<String, String>) {
		nonFatals += NonFatal(throwable, context, keys.toMap())
	}

	override fun breadcrumb(message: String) {
		breadcrumbs += message
	}

	override fun setCollectionEnabled(enabled: Boolean) = Unit

	override fun setUserId(userId: String?) {
		this.currentUserId = userId
	}
}

/** Runs the block and remembers the traces; the block's result or exception passes through untouched. */
class FakePerfTracer : PerfTracer {
	val traces = CopyOnWriteArrayList<String>()

	override fun setCollectionEnabled(enabled: Boolean) = Unit

	override suspend fun <T> measure(name: String, attributes: Map<String, String>, block: suspend () -> T): T {
		traces += name
		return block()
	}
}
