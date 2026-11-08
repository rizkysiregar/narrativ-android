package com.rizkysiregar.feedzmore.ui.feature.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rizkysiregar.feedzmore.constants.OAuthConfig
import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.cache.JwtTokenHolder
import com.rizkysiregar.feedzmore.data.session.SessionManager
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.LoginRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.RegisterRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.feedzmore.domain.auth.GoogleLoginUseCase
import com.rizkysiregar.feedzmore.domain.auth.LoginUseCase
import com.rizkysiregar.feedzmore.domain.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val registerUseCase: RegisterUseCase,
    private val authService: AuthorizationService,
    private val dataStore: DataStoreManager,
    private val tokenHolder: JwtTokenHolder,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // state for register
    private val _registerState = MutableStateFlow<ResultState<String>?>(null)
    val registerState: StateFlow<ResultState<String>?> = _registerState

    // state for login
    private val _loginState = MutableStateFlow<ResultState<LoginResponse>?>(null)
    val loginState: StateFlow<ResultState<LoginResponse>?> = _loginState

    // state for google login
    private val _googleLoginState = MutableStateFlow<ResultState<LoginResponse>?>(null)
    val googleLoginState: StateFlow<ResultState<LoginResponse>?> = _googleLoginState


    fun register(registerRequest: RegisterRequest) {
        _registerState.value = ResultState.Loading
        viewModelScope.launch {
            registerUseCase(registerRequest).collect { result ->
                _registerState.value = result
            }
        }
    }
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

    private suspend fun getGoogleIdToken(): String {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(OAuthConfig.WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        // check type before casting
        return when {
            credential is GoogleIdTokenCredential -> {
                credential.idToken
            }

            credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                // wrap as CustomCredential - this is what you hitting
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                googleCredential.idToken
            }

            else -> {
                throw Exception("Unexpected credentials type: ${credential.type}")
            }
        }
    }


    fun resetLoginState() {
        _loginState.value = ResultState.Idle
    }

    fun resetGoogleLoginState() {
        _googleLoginState.value = ResultState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = ResultState.Idle
    }

    private suspend fun saveSession(data: LoginResponse) {
        dataStore.put(PrefKeys.token, data.token)
        dataStore.put(PrefKeys.refreshToken, data.refreshToken)
        dataStore.put(PrefKeys.username, data.username)
        dataStore.put(PrefKeys.userId, data.userId)
        dataStore.put(PrefKeys.isLoggedIn, true)
        tokenHolder.setToken(data.token)
        sessionManager.markAuthenticated()
    }

}

