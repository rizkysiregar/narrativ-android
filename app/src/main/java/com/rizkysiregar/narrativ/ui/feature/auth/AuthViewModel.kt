package com.rizkysiregar.narrativ.ui.feature.auth

import android.util.Log
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.narrativ.constants.PrefKeys
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.remote.DeviceSyncScheduler
import com.rizkysiregar.narrativ.data.cache.JwtTokenHolder
import com.rizkysiregar.narrativ.data.session.GoogleIdTokenProvider
import com.rizkysiregar.narrativ.data.session.SessionManager
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.auth.LoginRequest
import com.rizkysiregar.narrativ.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.narrativ.domain.auth.GoogleLoginUseCase
import com.rizkysiregar.narrativ.domain.auth.LoginUseCase
import com.rizkysiregar.narrativ.telemetry.AnalyticsEvents
import com.rizkysiregar.narrativ.telemetry.AppAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.rizkysiregar.narrativ.i18n.AppStrings
import com.rizkysiregar.narrativ.R

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val dataStore: DataStoreManager,
    private val tokenHolder: JwtTokenHolder,
    private val sessionManager: SessionManager,
    private val deviceSyncScheduler: DeviceSyncScheduler,
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    private val analytics: AppAnalytics,
    private val appStrings: AppStrings
) : ViewModel() {

    // state for login
    private val _loginState = MutableStateFlow<ResultState<LoginResponse>?>(null)
    val loginState: StateFlow<ResultState<LoginResponse>?> = _loginState

    // state for google login
    private val _googleLoginState = MutableStateFlow<ResultState<LoginResponse>?>(null)
    val googleLoginState: StateFlow<ResultState<LoginResponse>?> = _googleLoginState


    fun login(username: String, password: String) {
        _loginState.value = ResultState.Loading
        viewModelScope.launch {
            loginUseCase(LoginRequest(username, password))
                .collect { result ->

                    if (result is ResultState.Success) {
                        // save token and necessary user info into datastore
                        saveSession(result.data)
                        analytics.log(AnalyticsEvents.LOGIN, AnalyticsEvents.Param.METHOD to "password")
                    } else if (result is ResultState.Failure) {
                        analytics.log(AnalyticsEvents.LOGIN_FAILED, AnalyticsEvents.Param.METHOD to "password")
                    }

                    _loginState.value = result
                }
        }
    }

    fun signInWithGoogle() {
        _googleLoginState.value = ResultState.Loading
        viewModelScope.launch {
            try {
                val idToken = getGoogleIdToken()
                googleLoginUseCase(idToken).collect { result ->
                    if (result is ResultState.Success) {
                        saveSession(result.data)
                        analytics.log(AnalyticsEvents.LOGIN, AnalyticsEvents.Param.METHOD to "google")
                    } else if (result is ResultState.Failure) {
                        analytics.log(AnalyticsEvents.LOGIN_FAILED, AnalyticsEvents.Param.METHOD to "google")
                    }
                    _googleLoginState.value = result
                }
            } catch (gce: GetCredentialException) {
                println("error.gce: ${gce.message}")
                _googleLoginState.value =
                    ResultState.Failure(gce.message ?: appStrings.get(R.string.error_google_sign_in))
            } catch (e: Exception) {
                println("error.e: ${e.message}")
                _googleLoginState.value = ResultState.Failure(e.message ?: appStrings.get(R.string.error_generic))
            }
        }
    }

    private suspend fun getGoogleIdToken(): String = googleIdTokenProvider.get()


    fun resetLoginState() {
        _loginState.value = ResultState.Idle
    }

    fun resetGoogleLoginState() {
        _googleLoginState.value = ResultState.Idle
    }

    private suspend fun saveSession(data: LoginResponse) {
        dataStore.put(PrefKeys.token, data.token)
        dataStore.put(PrefKeys.refreshToken, data.refreshToken)
        dataStore.put(PrefKeys.username, data.username)
        dataStore.put(PrefKeys.userId, data.userId)
        dataStore.put(PrefKeys.isLoggedIn, true)
        tokenHolder.setToken(data.token)
        sessionManager.markAuthenticated()
        sessionManager.identify(data.userId)
        deviceSyncScheduler.schedule(force = true)
    }

}

