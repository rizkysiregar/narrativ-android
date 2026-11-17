package com.rizkysiregar.narrativ.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** The wall clock, injected so tests can move time instead of waiting for it. */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {

	@Provides
	fun provideClock(): Clock = Clock.systemUTC()
}
