package com.rizkysiregar.narrativ.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

	@Provides
	@IoDispatcher
	fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

	// SupervisorJob: one failed child must not cancel the work launched after it.
	@Provides
	@Singleton
	@ApplicationScope
	fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
