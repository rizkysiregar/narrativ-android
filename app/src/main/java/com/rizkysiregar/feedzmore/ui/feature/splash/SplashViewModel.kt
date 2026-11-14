package com.rizkysiregar.feedzmore.ui.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.cache.JwtTokenHolder
import com.rizkysiregar.feedzmore.data.cache.OnboardingPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */

@HiltViewModel
class SplashViewModel @Inject constructor(
	private val dataStore: DataStoreManager,
	private val tokenHolder: JwtTokenHolder,
	onboardingPrefs: OnboardingPrefs
) : ViewModel() {

	val onboardingSeen: Boolean = onboardingPrefs.isSeen()

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

}