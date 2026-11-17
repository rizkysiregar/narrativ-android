package com.rizkysiregar.narrativ.di

import com.rizkysiregar.narrativ.config.FirebaseRemoteConfigManager
import com.rizkysiregar.narrativ.config.RemoteConfigManager
import com.rizkysiregar.narrativ.i18n.AppStrings
import com.rizkysiregar.narrativ.i18n.ContextAppStrings
import com.rizkysiregar.narrativ.telemetry.AppAnalytics
import com.rizkysiregar.narrativ.telemetry.CrashReporter
import com.rizkysiregar.narrativ.telemetry.FirebaseAppAnalytics
import com.rizkysiregar.narrativ.telemetry.FirebaseCrashReporter
import com.rizkysiregar.narrativ.telemetry.FirebasePerfTracer
import com.rizkysiregar.narrativ.telemetry.PerfTracer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The SDK-backed services behind interfaces, so tests can swap them for fakes. */
@Module
@InstallIn(SingletonComponent::class)
abstract class TelemetryModule {

	@Binds
	@Singleton
	abstract fun bindAnalytics(impl: FirebaseAppAnalytics): AppAnalytics

	@Binds
	@Singleton
	abstract fun bindCrashReporter(impl: FirebaseCrashReporter): CrashReporter

	@Binds
	@Singleton
	abstract fun bindPerfTracer(impl: FirebasePerfTracer): PerfTracer

	@Binds
	@Singleton
	abstract fun bindAppStrings(impl: ContextAppStrings): AppStrings

	@Binds
	@Singleton
	abstract fun bindRemoteConfig(impl: FirebaseRemoteConfigManager): RemoteConfigManager
}
