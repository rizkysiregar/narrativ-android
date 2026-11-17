package com.rizkysiregar.narrativ.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Text for code that has no composition to read it from (view models, workers, notifications). It always reads in the
 * current [AppLocale], which an application context does not follow on its own before Android 13.
 */
interface AppStrings {
	fun get(@StringRes id: Int, vararg args: Any): String

	companion object {
		fun localized(context: Context): Context {
			val config = Configuration(context.resources.configuration)
			config.setLocale(AppLocale.locale())
			return context.createConfigurationContext(config)
		}
	}
}

@Singleton
class ContextAppStrings @Inject constructor(@ApplicationContext private val context: Context) : AppStrings {

	override fun get(@StringRes id: Int, vararg args: Any): String =
		AppStrings.localized(context).getString(id, *args)
}
