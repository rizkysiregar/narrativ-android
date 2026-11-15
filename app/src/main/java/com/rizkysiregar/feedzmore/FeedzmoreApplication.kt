package com.rizkysiregar.feedzmore

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import com.rizkysiregar.feedzmore.config.RemoteConfigManager
import com.rizkysiregar.feedzmore.data.remote.DeviceSyncScheduler
import com.rizkysiregar.feedzmore.notification.UploadNotifications
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FeedzmoreApplication : Application(), Configuration.Provider {

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	@Inject
	lateinit var deviceSyncScheduler: DeviceSyncScheduler

	@Inject
	lateinit var remoteConfigManager: RemoteConfigManager

	override fun onCreate() {
		super.onCreate()
		UploadNotifications.createChannels(this)

		// Every time the app comes to the foreground, silently sync device info and refresh Remote Config
		// (Firebase itself throttles the real fetches to once per interval).
		ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
			override fun onStart(owner: LifecycleOwner) {
				deviceSyncScheduler.schedule()
				owner.lifecycleScope.launch { remoteConfigManager.refresh() }
			}
		})
	}

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()
}
