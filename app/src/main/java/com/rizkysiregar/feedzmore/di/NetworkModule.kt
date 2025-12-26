package com.rizkysiregar.feedzmore.di

import com.rizkysiregar.feedzmore.BuildConfig
import com.rizkysiregar.feedzmore.data.remote.AuthInterceptor
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


	@Provides
	@Singleton
	fun provideOkHttpClient(
		authInterceptor: AuthInterceptor
	): OkHttpClient {
		val loggingInterceptor = HttpLoggingInterceptor().apply {
			level = if (BuildConfig.DEBUG)
				HttpLoggingInterceptor.Level.BODY
			else
				HttpLoggingInterceptor.Level.NONE
		}

		return OkHttpClient.Builder()
			.addInterceptor(loggingInterceptor)
			.addInterceptor(authInterceptor)
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.build()
	}

	@Provides
	@Singleton
	fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
		return Retrofit.Builder()
//			.baseUrl(Constants.BASE_URL)
			.baseUrl(BuildConfig.BASE_URL)
			.addConverterFactory(GsonConverterFactory.create())
			.client(okHttpClient)
			.build()
	}
}