package com.rizkysiregar.narrativ

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import com.rizkysiregar.narrativ.appcheck.AppCheckInstaller
import com.rizkysiregar.narrativ.config.RemoteConfigManager
import com.rizkysiregar.narrativ.data.remote.DeviceSyncScheduler
import com.rizkysiregar.narrativ.notification.UploadNotifications
import com.rizkysiregar.narrativ.telemetry.TelemetrySettings
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.rizkysiregar.narrativ.data.remote.ApiMessages

@HiltAndroidApp
class NarrativApplication : Application(), Configuration.Provider {

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	@Inject
	lateinit var deviceSyncScheduler: DeviceSyncScheduler

	@Inject
	lateinit var remoteConfigManager: RemoteConfigManager

	@Inject
	lateinit var telemetrySettings: TelemetrySettings

	override fun onCreate() {
		super.onCreate()
		ApiMessages.init(this)
		AppCheckInstaller.install()
		telemetrySettings.apply()
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
