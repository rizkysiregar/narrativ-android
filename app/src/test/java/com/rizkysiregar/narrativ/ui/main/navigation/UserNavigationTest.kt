package com.rizkysiregar.narrativ.ui.main.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class UserNavigationTest {

	@Test
	fun yourselfOpensTheProfileTab() {
		assertEquals(BottomNavScreen.Profile.route, userRoute("u1", "ani", currentUserId = "u1"))
	}

	@Test
	fun someoneElseOpensTheirDetail() {
		assertEquals("user_detail/u2/budi", userRoute("u2", "budi", currentUserId = "u1"))
	}

	@Test
	fun beforeTheSessionIsReadEveryoneOpensTheDetail() {
		assertEquals("user_detail/u1/ani", userRoute("u1", "ani", currentUserId = ""))
	}
}
