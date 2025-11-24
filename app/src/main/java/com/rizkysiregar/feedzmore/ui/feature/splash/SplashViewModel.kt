package com.rizkysiregar.feedzmore.ui.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.cache.JwtTokenHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
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
	private val dataStore: DataStoreManager, private val tokenHolder: JwtTokenHolder
) : ViewModel() {

	init {
		// ---  load jwt token authentication --- //
		viewModelScope.launch {
			dataStore.get(PrefKeys.token, "").collect { token ->
				tokenHolder.setToken(token)
			}
		}

	}

	val isLoggedIn = dataStore.get(PrefKeys.isLoggedIn, false)
		.stateIn(viewModelScope, SharingStarted.Eagerly, false)

}