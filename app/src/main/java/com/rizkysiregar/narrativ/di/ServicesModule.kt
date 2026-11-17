package com.rizkysiregar.narrativ.di

import com.rizkysiregar.narrativ.data.remote.DeviceSyncScheduler
import com.rizkysiregar.narrativ.data.remote.WorkDeviceSyncScheduler
import com.rizkysiregar.narrativ.data.session.CredentialManagerGoogleIdTokenProvider
import com.rizkysiregar.narrativ.data.session.FirebaseRemoteSessionCleaner
import com.rizkysiregar.narrativ.data.session.GoogleIdTokenProvider
import com.rizkysiregar.narrativ.data.session.RemoteSessionCleaner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Services that talk to the system or to Google, behind interfaces so view model tests can fake them. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ServicesModule {

	@Binds
	@Singleton
	abstract fun bindDeviceSyncScheduler(impl: WorkDeviceSyncScheduler): DeviceSyncScheduler

	@Binds
	@Singleton
	abstract fun bindRemoteSessionCleaner(impl: FirebaseRemoteSessionCleaner): RemoteSessionCleaner

	@Binds
	@Singleton
	abstract fun bindGoogleIdTokenProvider(impl: CredentialManagerGoogleIdTokenProvider): GoogleIdTokenProvider
}
