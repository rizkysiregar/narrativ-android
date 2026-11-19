package com.rizkysiregar.narrativ.di

import com.rizkysiregar.narrativ.data.repository.AuthRepository
import com.rizkysiregar.narrativ.data.repository.FeedRepository
import com.rizkysiregar.narrativ.data.repository.NotificationRepository
import com.rizkysiregar.narrativ.data.repository.ModerationRepository
import com.rizkysiregar.narrativ.data.repository.PostRepository
import com.rizkysiregar.narrativ.data.repository.UserRepository
import com.rizkysiregar.narrativ.data.repository.impl.AuthRepositoryImpl
import com.rizkysiregar.narrativ.data.repository.impl.FeedRepositoryImpl
import com.rizkysiregar.narrativ.data.repository.impl.NotificationRepositoryImpl
import com.rizkysiregar.narrativ.data.repository.impl.ModerationRepositoryImpl
import com.rizkysiregar.narrativ.data.repository.impl.PostRepositoryImpl
import com.rizkysiregar.narrativ.data.repository.impl.UserRepositoryImpl
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
	abstract fun bindModerationRepository(impl: ModerationRepositoryImpl): ModerationRepository

	@Binds
	@Singleton
	abstract fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

	@Binds
	@Singleton
	abstract fun bindFeedPostRepository(feedRepositoryImpl: FeedRepositoryImpl): FeedRepository

	@Binds
	@Singleton
	abstract fun bindPostRepository(postRepositoryImpl: PostRepositoryImpl): PostRepository

	@Binds
	@Singleton
	abstract fun bindUserRepository(userRepositoryImpl: UserRepositoryImpl): UserRepository

	@Binds
	@Singleton
	abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

}