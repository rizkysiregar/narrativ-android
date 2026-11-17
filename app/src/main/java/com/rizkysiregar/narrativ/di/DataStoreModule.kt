package com.rizkysiregar.narrativ.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */


@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

	/**
	 * The name of the preferences file. It holds the JWT and refresh token, so renaming it signs every user out
	 * after an update. It is the same file `preferencesDataStore("app_pref")` used before.
	 */
	const val DATASTORE_NAME = "app_pref"

	@Provides
	@Singleton
	fun providePreferencesDataStore(
		@ApplicationContext context: Context
	): DataStore<Preferences> = PreferenceDataStoreFactory.create(
		produceFile = { context.preferencesDataStoreFile(DATASTORE_NAME) }
	)
}
