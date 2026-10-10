package com.rizkysiregar.feedzmore

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rizkysiregar.feedzmore.notification.UploadNotifications
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FeedzmoreApplication : Application(), Configuration.Provider {

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	override fun onCreate() {
		super.onCreate()
		UploadNotifications.createChannels(this)
	}

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()
}
