package com.rizkysiregar.narrativ.di

import javax.inject.Qualifier

/** The dispatcher for blocking I/O. Tests replace it with a test dispatcher so delays run on virtual time. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** A scope that lives as long as the process, for work that must outlive any screen. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
