package com.rizkysiregar.narrativ.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 */

@Singleton
class DataStoreManager @Inject constructor(
	private val dataStore: DataStore<Preferences>
) {

	// Generic save for any type
	suspend fun <T> put(key: Preferences.Key<T>, value: T) {
		dataStore.edit { prefs ->
			prefs[key] = value
		}
	}


	// Generic read for any type
	fun <T> get(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
		return dataStore.data.map { prefs ->
			prefs[key] ?: defaultValue
		}
	}

	suspend fun delete(key: Preferences.Key<*>) {
		dataStore.edit { prefs ->
			prefs.remove(key)
		}
	}

	suspend fun clearAll() {
		dataStore.edit { it.clear() }
	}
}
