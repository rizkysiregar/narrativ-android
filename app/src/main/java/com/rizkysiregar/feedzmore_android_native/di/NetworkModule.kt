package com.rizkysiregar.feedzmore_android_native.di

import com.rizkysiregar.feedzmore_android_native.BuildConfig
import com.rizkysiregar.feedzmore_android_native.data.remote.AuthInterceptor
import com.rizkysiregar.feedzmore_android_native.data.remote.api.AuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/08/25
 * Description: Software Engineer
 */

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

	private const val BASE_URL = "http://192.168.1.17:9091/api/v1/"

	@Provides
	@Singleton
	fun provideOkHttpClient(): OkHttpClient {
		val loggingInterceptor = HttpLoggingInterceptor().apply {
			level = if (BuildConfig.DEBUG)
				HttpLoggingInterceptor.Level.BODY
			else
				HttpLoggingInterceptor.Level.NONE
		}

		return OkHttpClient.Builder()
			.addInterceptor(loggingInterceptor)
			.addInterceptor(AuthInterceptor { null }) // later add token provider here
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.build()
	}

	@Provides
	@Singleton
	fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
		return Retrofit.Builder()
			.baseUrl(BASE_URL)
			.addConverterFactory(GsonConverterFactory.create())
			.client(okHttpClient)
			.build()
	}

	@Provides
	@Singleton
	fun provideAuthApi(retrofit: Retrofit): AuthApi {
		return retrofit.create(AuthApi::class.java)
	}



}