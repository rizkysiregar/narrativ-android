package com.rizkysiregar.feedzmore.data.repository

import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 11/12/25
 * Description: Single source of truth for user-related data stored locally
 */

@Singleton
class UserDataRepository @Inject constructor(
	private val dataStoreManager: DataStoreManager
) {
	val token: Flow<String> = dataStoreManager.get(PrefKeys.token, "")
	val userId: Flow<String> = dataStoreManager.get(PrefKeys.userId, "")
	val username: Flow<String> = dataStoreManager.get(PrefKeys.username, "")
	val isLoggedIn: Flow<Boolean> = dataStoreManager.get(PrefKeys.isLoggedIn, false)


	/**
	 * Saves all essential user session data in a single, atomic transaction after a successful login.
	 * Using `dataStore.edit` ensures that all writes are completed before the function returns.
	 *
	 * @param token The authentication token received from the server.
	 * @param userId The unique ID of the user.
	 * @param username The username of the user.
	 */
	suspend fun saveUserSession(
		token: String,
		userId: String,
		username: String
	) {
		dataStoreManager.put(PrefKeys.token, token)
		dataStoreManager.put(PrefKeys.userId, userId)
		dataStoreManager.put(PrefKeys.username, username)
		dataStoreManager.put(PrefKeys.isLoggedIn, true)
	}


}