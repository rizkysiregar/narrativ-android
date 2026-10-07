package com.rizkysiregar.feedzmore.util

import android.util.Log
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.BaseResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
		if (e is CancellationException) throw e
		Log.e("safeApiCall", "Exception: $e")
		ResultState.Failure("Something went wrong, please try again later")
	}
}

fun <T> flowFromApi(
	apiCall: suspend () -> BaseResponse<T>
): Flow<ResultState<T>> = flow {
	emit(ResultState.Loading)

	val result = safeApiCall { apiCall() }
	when (result) {
		is ResultState.Success -> {
			val response = result.data
			if (response.data != null) {
				emit(ResultState.Success(response.data))
			} else {
				emit(ResultState.Failure(response.message))
			}
		}

		is ResultState.Failure -> emit(result)
		else -> { // di nothing }
		}
	}
}
