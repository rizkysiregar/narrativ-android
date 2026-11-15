package com.rizkysiregar.feedzmore.data.remote

import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rizkysiregar.feedzmore.BuildConfig
import com.rizkysiregar.feedzmore.appcheck.AppCheckInterceptor
import com.rizkysiregar.feedzmore.constants.PrefKeys
import com.rizkysiregar.feedzmore.data.cache.DataStoreManager
import com.rizkysiregar.feedzmore.data.cache.JwtTokenHolder
import com.rizkysiregar.feedzmore.data.remote.model.request.auth.RefreshTokenRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.auth.LoginResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exchanges the stored refresh token for a new token pair.
 *
 * Refresh tokens are single use, so calls are serialized: when several requests hit
 * 401 at once, only the first performs the refresh and the rest reuse its result.
 * It uses its own bare client so the call never passes through [AuthInterceptor].
 */
@Singleton
class TokenRefresher @Inject constructor(
	private val dataStore: DataStoreManager,
	private val tokenHolder: JwtTokenHolder,
	chuckerInterceptor: ChuckerInterceptor,
	appCheckInterceptor: AppCheckInterceptor
) {
	enum class Result { REFRESHED, REJECTED, FAILED }

	private val client = OkHttpClient.Builder()
		.addInterceptor(appCheckInterceptor)
		.addInterceptor(chuckerInterceptor)
		.connectTimeout(30, TimeUnit.SECONDS)
		.readTimeout(30, TimeUnit.SECONDS)
		.build()
	private val gson = Gson()

	/**
	 * @param rejectedToken the access token that got the 401, used to detect that
	 * another request already refreshed in the meantime.
	 */
	@Synchronized
	fun refresh(rejectedToken: String?): Result {
		val current = tokenHolder.getToken()
		if (!current.isNullOrEmpty() && current != rejectedToken) return Result.REFRESHED

		val refreshToken = runBlocking { dataStore.get(PrefKeys.refreshToken, "").first() }
		if (refreshToken.isEmpty()) return Result.REJECTED

		val body = gson.toJson(RefreshTokenRequest(refreshToken))
			.toRequestBody("application/json".toMediaType())
		val request = Request.Builder()
			.url(BuildConfig.BASE_URL + "auth/refresh")
			.post(body)
			.build()

		return try {
			client.newCall(request).execute().use { response ->
				when {
					response.code == 401 -> Result.REJECTED
					!response.isSuccessful -> Result.FAILED
					else -> {
						val type = object : TypeToken<BaseResponse<LoginResponse>>() {}.type
						val data = gson.fromJson<BaseResponse<LoginResponse>>(
							response.body?.string(), type
						)?.data ?: return Result.FAILED
						runBlocking {
							dataStore.put(PrefKeys.token, data.token)
							dataStore.put(PrefKeys.refreshToken, data.refreshToken)
						}
						tokenHolder.setToken(data.token)
						Result.REFRESHED
					}
				}
			}
		} catch (e: IOException) {
			Result.FAILED
		} catch (e: RuntimeException) {
			Result.FAILED
		}
	}
}
