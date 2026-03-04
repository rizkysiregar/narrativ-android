package com.rizkysiregar.feedzmore.ui.feature.auth

import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.constants.OAuthConfig
import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.cache.JwtTokenHolder
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.LoginRequest
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.RegisterRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.feedzmore.domain.auth.LoginUseCase
import com.rizkysiregar.feedzmore.domain.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
	private val loginUseCase: LoginUseCase,
	private val registerUseCase: RegisterUseCase,
	private val authService: AuthorizationService,
	private val dataStore: DataStoreManager,
	private val tokenHolder: JwtTokenHolder
) : ViewModel() {

	// state for register
	private val _registerState = MutableStateFlow<ResultState<String>?>(null)
	val registerState: StateFlow<ResultState<String>?> = _registerState

	// state for login
	private val _loginState = MutableStateFlow<ResultState<LoginResponse>?>(null)
	val loginState: StateFlow<ResultState<LoginResponse>?> = _loginState


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
						dataStore.put(PrefKeys.token, result.data.token)
						dataStore.put(PrefKeys.username, result.data.username)
						dataStore.put(PrefKeys.userId, result.data.userId)
						dataStore.put(PrefKeys.isLoggedIn, true)
						tokenHolder.setToken(result.data.token)
					}

					_loginState.value = result
				}
		}
	}

	fun resetLoginState() {
		_loginState.value = ResultState.Idle
	}

	fun resetRegisterState() {
		_registerState.value = ResultState.Idle
	}


	// OAuth login
	fun buildLoginGoogleIntent(): Intent {
		val serviceConfig = AuthorizationServiceConfiguration(
			OAuthConfig.AUTH_URL.toUri(),
			OAuthConfig.TOKEN_URL.toUri()
		)

		val authRequest = AuthorizationRequest.Builder(
			serviceConfig,
			OAuthConfig.CLIENT_ID,
			ResponseTypeValues.CODE,
			OAuthConfig.REDIRECT_URI.toUri()
		)
			.setScope("openid profile email")
			.build()
		Log.d("APP AUTH", "Auth URL: ${authRequest.toUri()}")

		return authService.getAuthorizationRequestIntent(authRequest)
	}

	fun handleAuthResponse(intent: Intent) {

	}


}


