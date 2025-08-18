package com.rizkysiregar.feedzmore_android_native.ui.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore_android_native.data.remote.model.LoginRequest
import com.rizkysiregar.feedzmore_android_native.data.remote.model.LoginResponse
import com.rizkysiregar.feedzmore_android_native.data.remote.model.ResultState
import com.rizkysiregar.feedzmore_android_native.domain.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
	private val loginUseCase: LoginUseCase
) : ViewModel() {

	private val _loginState = MutableStateFlow<ResultState<LoginResponse>?>(null)
	val loginState: StateFlow<ResultState<LoginResponse>?> = _loginState


	fun login(username: String, password: String) {
		viewModelScope.launch {
			loginUseCase(LoginRequest(username, password))
				.collect { result ->
					_loginState.value = result
				}
		}
	}
}
