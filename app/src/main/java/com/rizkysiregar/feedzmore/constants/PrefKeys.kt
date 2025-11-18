package com.rizkysiregar.feedzmore.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */
object PrefKeys {
	val isLoggedIn = booleanPreferencesKey("is_logged_in")
	val token = stringPreferencesKey("token")
	val username = stringPreferencesKey("username")
}