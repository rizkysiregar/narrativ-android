package com.rizkysiregar.narrativ.config

import android.content.Context
import android.util.Log
import com.rizkysiregar.narrativ.data.remote.api.ConfigApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The feature switches an admin flips in App management (backend GET /config/client), so a feature that is not
 * ready can be hidden without a release.
 */
interface FeatureFlags {
	/** Chat: the messages icon, the Message button on profiles and chat notifications. */
	val chatEnabled: StateFlow<Boolean>

	/** Asks the backend again. A failure keeps the last known value. */
	suspend fun refresh()
}

/**
 * The last answer is kept in SharedPreferences, read synchronously at start: the push service may run before the app
 * asked the backend, and a cold start should not flash the chat icon in or out. Outside the DataStore on purpose, as
 * the switches belong to the app, not to the user: [com.rizkysiregar.narrativ.data.cache.DataStoreManager.clearAll]
 * runs on logout. Nothing known yet means off.
 */
@Singleton
class ApiFeatureFlags @Inject constructor(
	retrofit: Retrofit,
	@ApplicationContext context: Context
) : FeatureFlags {
	private val api: ConfigApi by lazy { retrofit.create(ConfigApi::class.java) }
	private val prefs = context.getSharedPreferences("feature_flags", Context.MODE_PRIVATE)
	private val _chatEnabled = MutableStateFlow(prefs.getBoolean(KEY_CHAT, false))
	override val chatEnabled: StateFlow<Boolean> = _chatEnabled.asStateFlow()

	override suspend fun refresh() {
		try {
			val chat = api.clientConfig().data?.features?.chat == true
			prefs.edit().putBoolean(KEY_CHAT, chat).apply()
			_chatEnabled.value = chat
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			Log.w(TAG, "Feature switches not refreshed, keeping the last known ones", e)
		}
	}

	private companion object {
		const val TAG = "FeatureFlags"
		const val KEY_CHAT = "chat"
	}
}
