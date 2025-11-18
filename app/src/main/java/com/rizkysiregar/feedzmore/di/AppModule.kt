package com.rizkysiregar.feedzmore.di

import com.rizkysiregar.feedzmore.data.repository.AuthRepository
import com.rizkysiregar.feedzmore.data.repository.impl.AuthRepositoryImpl
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

}