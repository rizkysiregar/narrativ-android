package com.rizkysiregar.narrativ.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.openid.appauth.AuthorizationService
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

	@Provides
	@Singleton
	fun provideAuthorizationService(
		@ApplicationContext context: Context
	): AuthorizationService {
		return AuthorizationService(context)
	}

}