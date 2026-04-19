package com.rizkysiregar.feedzmore.domain.auth

import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.GoogleLoginRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.LoginResponse
import com.rizkysiregar.feedzmore.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GoogleLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(idToken: String): Flow<ResultState<LoginResponse>> {
        return authRepository.googleLogin(GoogleLoginRequest(idToken))
    }
}