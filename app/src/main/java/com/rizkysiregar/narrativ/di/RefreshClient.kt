package com.rizkysiregar.narrativ.di

import javax.inject.Qualifier

/**
 * Marks the OkHttp client of the token refresh call. It is kept apart from the main client on purpose:
 * it has no auth interceptor, so a refresh can never trigger another refresh.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshClient
