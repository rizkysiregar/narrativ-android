package com.rizkysiregar.narrativ.di

import javax.inject.Qualifier

/** The API base URL, with its trailing slash (e.g. `https://host/api/v1/`). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl
