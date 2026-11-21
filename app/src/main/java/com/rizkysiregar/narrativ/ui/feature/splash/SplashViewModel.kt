package com.rizkysiregar.narrativ.ui.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.config.RemoteConfigManager
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.cache.JwtTokenHolder
import com.rizkysiregar.narrativ.data.cache.OnboardingPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 */

@HiltViewModel
class SplashViewModel @Inject constructor(
	private val dataStore: DataStoreManager,
	private val tokenHolder: JwtTokenHolder,
	private val remoteConfigManager: RemoteConfigManager,
	onboardingPrefs: OnboardingPrefs
) : ViewModel() {

	val onboardingSeen: Boolean = onboardingPrefs.isSeen()

	/** Gives Remote Config a moment to answer so a required update shows before the home screen does. */
	suspend fun awaitRemoteConfig() {
		withTimeoutOrNull(REMOTE_CONFIG_WAIT_MS) { remoteConfigManager.awaitFirstRefresh() }
	}

	init {
		// ---  load jwt token authentication --- //
		viewModelScope.launch {
			dataStore.get(PrefKeys.token, "").collect { token ->
				tokenHolder.setToken(token)
			}
		}

	}


	val isLoggedIn: StateFlow<Boolean?> = dataStore.get(PrefKeys.isLoggedIn, false)
		.map { isLoggedInValue -> isLoggedInValue as Boolean? }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = null
		)

	private companion object {
		const val REMOTE_CONFIG_WAIT_MS = 3000L
	}

}
