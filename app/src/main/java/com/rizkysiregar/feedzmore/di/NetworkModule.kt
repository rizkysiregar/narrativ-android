package com.rizkysiregar.feedzmore.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.rizkysiregar.feedzmore.BuildConfig
import com.rizkysiregar.feedzmore.appcheck.AppCheckInterceptor
import com.rizkysiregar.feedzmore.data.remote.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

	// Keeps large bodies (e.g. media uploads) from bloating the inspector database.
	private const val MAX_INSPECTED_BODY_BYTES = 100_000L

	@Provides
	@Singleton
	fun provideChuckerInterceptor(
		@ApplicationContext context: Context
	): ChuckerInterceptor {
		val collector = ChuckerCollector(
			context = context,
			showNotification = true,
			retentionPeriod = RetentionManager.Period.ONE_DAY
		)
		return ChuckerInterceptor.Builder(context)
			.collector(collector)
			.redactHeaders("Authorization", "Cookie", "Set-Cookie")
			.maxContentLength(MAX_INSPECTED_BODY_BYTES)
			.build()
	}

	@Provides
	@Singleton
	fun provideOkHttpClient(
		authInterceptor: AuthInterceptor,
		appCheckInterceptor: AppCheckInterceptor,
		chuckerInterceptor: ChuckerInterceptor
	): OkHttpClient {
		val loggingInterceptor = HttpLoggingInterceptor().apply {
			level = if (BuildConfig.DEBUG)
				HttpLoggingInterceptor.Level.BODY
			else
				HttpLoggingInterceptor.Level.NONE
		}

		return OkHttpClient.Builder()
			.addInterceptor(loggingInterceptor)
			.addInterceptor(appCheckInterceptor)
			.addInterceptor(authInterceptor)
			.addInterceptor(chuckerInterceptor)
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.writeTimeout(120, TimeUnit.SECONDS)
			.build()
	}

	@Provides
	@Singleton
	@UploadClient
	fun provideUploadOkHttpClient(chuckerInterceptor: ChuckerInterceptor): OkHttpClient {
		return OkHttpClient.Builder()
			.addInterceptor(chuckerInterceptor)
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.writeTimeout(5, TimeUnit.MINUTES)
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