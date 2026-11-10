package com.rizkysiregar.feedzmore.ui.feature.home.settings

import androidx.lifecycle.ViewModel
import com.rizkysiregar.feedzmore.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
	private val sessionManager: SessionManager
) : ViewModel() {

	fun logout() {
		sessionManager.logout()
	}
}
