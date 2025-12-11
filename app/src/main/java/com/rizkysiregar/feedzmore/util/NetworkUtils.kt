package com.rizkysiregar.feedzmore.util

import android.util.Log
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import org.json.JSONObject
import retrofit2.HttpException

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 20/11/25
 * Description: Software Engineer
 */

suspend fun <T> safeApiCall(
	apiCall: suspend () -> T
): ResultState<T> {
	return try {
		val result = apiCall()
		ResultState.Success(result)

	} catch (e: HttpException) {
		val errorBody = e.response()?.errorBody()?.string()
		val msg = try {
			JSONObject(errorBody ?: "").optString("message", "Unknown error")
		} catch (_: Exception) {
			"Something went wrong, please try again later"
		}

		ResultState.Failure(msg)
	} catch (e: Exception) {
		Log.e("safeApiCall", "Exception: $e")
		ResultState.Failure("Something went wrong, please try again later")
	}
}