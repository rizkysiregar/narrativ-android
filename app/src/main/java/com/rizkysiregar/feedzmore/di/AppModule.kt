package com.rizkysiregar.feedzmore.di

import com.rizkysiregar.feedzmore.data.repository.AuthRepository
import com.rizkysiregar.feedzmore.data.repository.FeedRepository
import com.rizkysiregar.feedzmore.data.repository.UserRepository
import com.rizkysiregar.feedzmore.data.repository.impl.AuthRepositoryImpl
import com.rizkysiregar.feedzmore.data.repository.impl.FeedRepositoryImpl
import com.rizkysiregar.feedzmore.data.repository.impl.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

	@Binds
	@Singleton
	abstract fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

	@Binds
	@Singleton
	abstract fun bindFeedPostRepository(feedRepositoryImpl: FeedRepositoryImpl): FeedRepository

	@Binds
	@Singleton
	abstract fun bindUserRepository(userRepositoryImpl: UserRepositoryImpl): UserRepository

}