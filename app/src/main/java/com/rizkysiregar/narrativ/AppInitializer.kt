package com.rizkysiregar.narrativ

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.rizkysiregar.narrativ.appcheck.AppCheckInstaller
import com.rizkysiregar.narrativ.config.RemoteConfigManager
import com.rizkysiregar.narrativ.data.remote.ApiMessages
import com.rizkysiregar.narrativ.data.remote.DeviceSyncScheduler
import com.rizkysiregar.narrativ.notification.PushNotifications
import com.rizkysiregar.narrativ.notification.UploadNotifications
import com.rizkysiregar.narrativ.telemetry.TelemetrySettings
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the app sets up when the process starts. It lives apart from [NarrativApplication] in three steps, so an
 * instrumented test (which runs in a HiltTestApplication, not in NarrativApplication) can take the local setup
 * and leave out Firebase and the foreground sync.
 */
@Singleton
class AppInitializer @Inject constructor(
	private val deviceSyncScheduler: DeviceSyncScheduler,
	private val remoteConfigManager: RemoteConfigManager,
	private val telemetrySettings: TelemetrySettings
) {

	fun initialize(context: Context) {
		initLocal(context)
		initServices()
		observeForeground()
	}

	/** Messages and notification channels: needs a context only. */
	fun initLocal(context: Context) {
		ApiMessages.init(context)
		UploadNotifications.createChannels(context)
		PushNotifications.createChannel(context)
	}

	/** The SDKs: App Check and the telemetry choice of the user. */
	fun initServices() {
		AppCheckInstaller.install()
		telemetrySettings.apply()
	}

	/**
	 * Every time the app comes to the foreground, silently sync device info and refresh Remote Config
	 * (Firebase itself throttles the real fetches to once per interval).
	 */
	fun observeForeground() {
		ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
			override fun onStart(owner: LifecycleOwner) {
				deviceSyncScheduler.schedule()
				owner.lifecycleScope.launch { remoteConfigManager.refresh() }
			}
		})
	}
}
