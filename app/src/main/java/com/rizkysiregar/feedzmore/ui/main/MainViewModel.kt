package com.rizkysiregar.feedzmore.ui.main

import androidx.lifecycle.ViewModel
import com.rizkysiregar.feedzmore.data.remote.ApiErrorEvent
import com.rizkysiregar.feedzmore.data.remote.ApiErrorManager
import com.rizkysiregar.feedzmore.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
	sessionManager: SessionManager,
	apiErrorManager: ApiErrorManager
) : ViewModel() {
	val sessionExpiredMessage: StateFlow<String?> = sessionManager.sessionExpiredMessage
	val apiErrors: SharedFlow<ApiErrorEvent> = apiErrorManager.errors
}
