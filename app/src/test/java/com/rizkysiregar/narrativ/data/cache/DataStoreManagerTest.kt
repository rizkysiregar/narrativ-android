package com.rizkysiregar.narrativ.data.cache

import com.google.common.truth.Truth.assertThat
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.di.DataStoreModule
import com.rizkysiregar.narrativ.testing.testDataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreManagerTest {

	@get:Rule
	val folder = TemporaryFolder()

	private fun TestScope.store() = testDataStoreManager(backgroundScope, folder.newFile("test.preferences_pb").also { it.delete() })

	@Test
	fun readingAnUnsetKeyGivesTheDefault() = runTest(UnconfinedTestDispatcher()) {
		val store = store()

		assertThat(store.get(PrefKeys.token, "").first()).isEqualTo("")
		assertThat(store.get(PrefKeys.isLoggedIn, false).first()).isFalse()
	}

	@Test
	fun aStoredValueIsReadBack() = runTest(UnconfinedTestDispatcher()) {
		val store = store()

		store.put(PrefKeys.token, "jwt")
		store.put(PrefKeys.deviceSyncAt, 42L)

		assertThat(store.get(PrefKeys.token, "").first()).isEqualTo("jwt")
		assertThat(store.get(PrefKeys.deviceSyncAt, 0L).first()).isEqualTo(42L)
	}

	@Test
	fun deleteRemovesOnlyThatKey() = runTest(UnconfinedTestDispatcher()) {
		val store = store()
		store.put(PrefKeys.token, "jwt")
		store.put(PrefKeys.refreshToken, "refresh")

		store.delete(PrefKeys.token)

		assertThat(store.get(PrefKeys.token, "").first()).isEmpty()
		assertThat(store.get(PrefKeys.refreshToken, "").first()).isEqualTo("refresh")
	}

	@Test
	fun clearAllRemovesEveryCredential() = runTest(UnconfinedTestDispatcher()) {
		val store = store()
		store.put(PrefKeys.token, "jwt")
		store.put(PrefKeys.refreshToken, "refresh")
		store.put(PrefKeys.userId, "u1")
		store.put(PrefKeys.isLoggedIn, true)

		store.clearAll()

		assertThat(store.get(PrefKeys.token, "").first()).isEmpty()
		assertThat(store.get(PrefKeys.refreshToken, "").first()).isEmpty()
		assertThat(store.get(PrefKeys.userId, "").first()).isEmpty()
		assertThat(store.get(PrefKeys.isLoggedIn, false).first()).isFalse()
	}

	/**
	 * These names are what is on disk on every installed device. Changing one reads as "not logged in" after the
	 * update, so a change here must come with a migration.
	 */
	@Test
	fun storedNamesStayTheSameAcrossVersions() {
		assertThat(DataStoreModule.DATASTORE_NAME).isEqualTo("app_pref")
		assertThat(
			listOf(
				PrefKeys.isLoggedIn, PrefKeys.token, PrefKeys.refreshToken, PrefKeys.username, PrefKeys.userId,
				PrefKeys.deviceSyncToken, PrefKeys.deviceSyncUserId, PrefKeys.deviceSyncAt
			).map { it.name }
		).containsExactly(
			"is_logged_in", "token", "refresh_token", "username", "user_id",
			"device_sync_token", "device_sync_user_id", "device_sync_at"
		).inOrder()
	}
}
