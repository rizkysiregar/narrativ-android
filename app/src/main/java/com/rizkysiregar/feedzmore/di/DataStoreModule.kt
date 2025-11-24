package com.rizkysiregar.feedzmore.di

import android.content.Context
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
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

	@Provides
	@Singleton
	fun provideDataStoreManager(
		@ApplicationContext context: Context
	): DataStoreManager = DataStoreManager(context)


}