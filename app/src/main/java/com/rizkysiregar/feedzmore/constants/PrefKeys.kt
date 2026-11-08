package com.rizkysiregar.feedzmore.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */
object PrefKeys {
	val isLoggedIn = booleanPreferencesKey("is_logged_in")
	val token = stringPreferencesKey("token")
	val refreshToken = stringPreferencesKey("refresh_token")
	val username = stringPreferencesKey("username")
	val userId = stringPreferencesKey("user_id")

	// Last successful POST /devices, used to throttle device sync.
	val deviceSyncToken = stringPreferencesKey("device_sync_token")
	val deviceSyncUserId = stringPreferencesKey("device_sync_user_id")
	val deviceSyncAt = longPreferencesKey("device_sync_at")
}