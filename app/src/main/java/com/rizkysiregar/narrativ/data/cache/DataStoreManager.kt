package com.rizkysiregar.narrativ.data.cache

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */

@Singleton
class DataStoreManager @Inject constructor(
	private val context: Context
) {
	private val Context.dataStore by preferencesDataStore("app_pref")

	// Generic save for any type
	suspend fun <T> put(key: Preferences.Key<T>, value: T) {
		context.dataStore.edit { prefs ->
			prefs[key] = value
		}
	}


	// Generic read for any type
	fun <T> get(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
		return context.dataStore.data.map { prefs ->
			prefs[key] ?: defaultValue
		}
	}

	suspend fun delete(key: Preferences.Key<*>) {
		context.dataStore.edit { prefs ->
			prefs.remove(key)
		}
	}

	suspend fun clearAll() {
		context.dataStore.edit { it.clear() }
	}
}