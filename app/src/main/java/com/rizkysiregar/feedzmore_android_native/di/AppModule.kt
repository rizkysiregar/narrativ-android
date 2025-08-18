package com.rizkysiregar.feedzmore_android_native.di

import com.rizkysiregar.feedzmore_android_native.data.repository.AuthRepository
import com.rizkysiregar.feedzmore_android_native.data.repository.impl.AuthRepositoryImpl
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