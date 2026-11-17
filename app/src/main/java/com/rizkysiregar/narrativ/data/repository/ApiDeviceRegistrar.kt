package com.rizkysiregar.narrativ.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.cache.JwtTokenHolder
import com.rizkysiregar.narrativ.data.remote.api.DeviceApi
import com.rizkysiregar.narrativ.data.remote.model.request.device.RegisterDeviceRequest
import com.rizkysiregar.narrativ.data.session.PushTokenProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import retrofit2.Retrofit
import com.rizkysiregar.narrativ.i18n.AppLocale
import java.time.Clock
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** [DeviceRegistrar] on `POST /devices`. */
@Singleton
class ApiDeviceRegistrar @Inject constructor(
	@ApplicationContext private val context: Context,
	private val dataStore: DataStoreManager,
	private val tokenHolder: JwtTokenHolder,
	private val pushTokens: PushTokenProvider,
	private val clock: Clock,
	retrofit: Retrofit
) : DeviceRegistrar {
	private val deviceApi: DeviceApi by lazy { retrofit.create(DeviceApi::class.java) }

	override suspend fun sync(force: Boolean): Boolean {
		return try {
			val userId = dataStore.get(PrefKeys.userId, "").first()
			val jwt = dataStore.get(PrefKeys.token, "").first()
			val loggedIn = dataStore.get(PrefKeys.isLoggedIn, false).first()
			Log.d(TAG, "sync(force=$force) loggedIn=$loggedIn userId=${userId.isNotEmpty()} jwt=${jwt.isNotEmpty()}")
			if (!loggedIn || userId.isEmpty() || jwt.isEmpty()) return true

			val pushToken = pushTokens.token()
			Log.d(TAG, "FCM token fetched: ${mask(pushToken)}")
			if (!force && isUpToDate(userId, pushToken)) {
				Log.d(TAG, "Skipped: already synced within $SYNC_INTERVAL_HOURS h")
				return true
			}

			// The worker can start before the splash screen has loaded the JWT.
			tokenHolder.setToken(jwt)
			val response = deviceApi.registerDevice(buildRequest(pushToken))
			Log.d(TAG, "POST /devices -> HTTP ${response.code()}")

			dataStore.put(PrefKeys.deviceSyncToken, pushToken)
			dataStore.put(PrefKeys.deviceSyncUserId, userId)
			dataStore.put(PrefKeys.deviceSyncAt, clock.millis())
			true
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			Log.w(TAG, "Device registration failed", e)
			false
		}
	}

	private suspend fun isUpToDate(userId: String, pushToken: String): Boolean {
		val age = clock.millis() - dataStore.get(PrefKeys.deviceSyncAt, 0L).first()
		return dataStore.get(PrefKeys.deviceSyncToken, "").first() == pushToken &&
			dataStore.get(PrefKeys.deviceSyncUserId, "").first() == userId &&
			age in 0 until TimeUnit.HOURS.toMillis(SYNC_INTERVAL_HOURS)
	}

	private fun mask(token: String) =
		if (token.length <= 10) "***" else "${token.take(6)}...${token.takeLast(4)} (len=${token.length})"

	private fun buildRequest(pushToken: String) = RegisterDeviceRequest(
		pushToken = pushToken,
		platform = PLATFORM,
		deviceId = androidId(),
		appVersion = appVersion(),
		osVersion = Build.VERSION.RELEASE,
		deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
		locale = AppLocale.current().tag,
		timezone = TimeZone.getDefault().id
	)

	@SuppressLint("HardwareIds")
	private fun androidId(): String? =
		Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

	private fun appVersion(): String? =
		runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
			.getOrNull()

	private companion object {
		const val TAG = "DeviceRegistrar"
		const val PLATFORM = "ANDROID"
		const val SYNC_INTERVAL_HOURS = 24L
	}
}
