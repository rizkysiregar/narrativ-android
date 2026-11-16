package com.rizkysiregar.narrativ.ui.feature.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ErrorDialogCoordinatorTest {

	private val coordinator = ErrorDialogCoordinator()
	private val dismissed = mutableListOf<String>()

	private fun entry(message: String, owner: String) =
		ErrorDialogCoordinator.Entry(message) { dismissed += owner }

	@Test
	fun onlyTheFirstDialogIsTheLeader() {
		val global = entry("Email sudah terdaftar", "global")
		val screen = entry("Email sudah terdaftar", "screen")
		coordinator.register(global)
		coordinator.register(screen)

		assertSame(global, coordinator.leader)
	}

	@Test
	fun oneDismissClearsEveryDialogWithTheSameMessage() {
		coordinator.register(entry("Email sudah terdaftar", "global"))
		coordinator.register(entry("Email sudah terdaftar", "screen"))

		coordinator.dismissLeader()

		assertEquals(listOf("global", "screen"), dismissed)
		assertNull(coordinator.leader)
	}

	@Test
	fun aDifferentMessageStaysAndIsShownNext() {
		coordinator.register(entry("Tidak ada koneksi", "global"))
		val other = entry("Email sudah terdaftar", "screen")
		coordinator.register(other)

		coordinator.dismissLeader()

		assertEquals(listOf("global"), dismissed)
		assertSame(other, coordinator.leader)
	}

	@Test
	fun anOwnerThatLeavesIsNoLongerTheLeader() {
		val global = entry("Tidak ada koneksi", "global")
		val screen = entry("Tidak ada koneksi", "screen")
		coordinator.register(global)
		coordinator.register(screen)

		coordinator.unregister(global)

		assertSame(screen, coordinator.leader)
	}

	@Test
	fun dismissingWithNothingShownDoesNothing() {
		coordinator.dismissLeader()

		assertEquals(emptyList<String>(), dismissed)
	}
}
