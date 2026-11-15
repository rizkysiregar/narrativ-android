package com.rizkysiregar.feedzmore.ui.feature.auth

import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.remote.DeviceSyncScheduler
import com.rizkysiregar.feedzmore.data.cache.JwtTokenHolder
import com.rizkysiregar.feedzmore.data.session.GoogleIdTokenProvider
import com.rizkysiregar.feedzmore.data.session.SessionManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.LoginRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.feedzmore.domain.auth.GoogleLoginUseCase
import com.rizkysiregar.feedzmore.domain.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val authService: AuthorizationService,
    private val dataStore: DataStoreManager,
    private val tokenHolder: JwtTokenHolder,
    private val sessionManager: SessionManager,
    private val deviceSyncScheduler: DeviceSyncScheduler,
    private val googleIdTokenProvider: GoogleIdTokenProvider
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
                    }
                    _googleLoginState.value = result
                }
            } catch (gce: GetCredentialException) {
                println("error.gce: ${gce.message}")
                _googleLoginState.value =
                    ResultState.Failure(gce.message ?: "Google Sign-In failed")
            } catch (e: Exception) {
                println("error.e: ${e.message}")
                _googleLoginState.value = ResultState.Failure(e.message ?: "Something went wrong")
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
        deviceSyncScheduler.schedule(force = true)
    }

}

