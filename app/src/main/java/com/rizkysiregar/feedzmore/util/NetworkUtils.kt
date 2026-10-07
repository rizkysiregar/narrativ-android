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
		ResultState.Failure(parseHttpErrorMessage(e))
	} catch (e: Exception) {
		if (e is CancellationException) throw e
		Log.e("safeApiCall", "Exception: $e")
		ResultState.Failure("Something went wrong, please try again later")
	}
}

private fun parseHttpErrorMessage(e: HttpException): String {
	val errorBody = e.response()?.errorBody()?.string()
	return try {
		JSONObject(errorBody ?: "").optString("message", "Unknown error")
	} catch (_: Exception) {
		"Something went wrong, please try again later"
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

fun <T> flowFromApiOrEmptyOn404(
	emptyValue: T,
	apiCall: suspend () -> BaseResponse<T>
): Flow<ResultState<T>> = flow {
	emit(ResultState.Loading)

	try {
		val response = apiCall()
		val data = response.data
		if (data != null) {
			emit(ResultState.Success(data))
		} else {
			emit(ResultState.Failure(response.message))
		}
	} catch (e: HttpException) {
		if (e.code() == 404) {
			emit(ResultState.Success(emptyValue))
		} else {
			emit(ResultState.Failure(parseHttpErrorMessage(e)))
		}
	} catch (e: Exception) {
		if (e is CancellationException) throw e
		Log.e("flowFromApiOrEmptyOn404", "Exception: $e")
		emit(ResultState.Failure("Something went wrong, please try again later"))
	}
}
