package com.rizkysiregar.narrativ.util

import android.util.Log
import com.google.gson.JsonParseException
import com.rizkysiregar.narrativ.data.remote.ApiMessages
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.BaseResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 20/11/25
 * Description: Software Engineer
 */

suspend fun <T> safeApiCall(
	apiCall: suspend () -> T
): ResultState<T> {
	return try {
		ResultState.Success(apiCall())
	} catch (e: Exception) {
		e.toFailure("safeApiCall")
	}
}

/**
 * Maps a failed API call to a user-facing [ResultState.Failure]. The original exception stays on
 * [ResultState.Failure.throwable] so callers can still tell HTTP, network and parsing errors apart
 * (HttpException, IOException, JsonParseException respectively).
 */
private fun Exception.toFailure(tag: String): ResultState.Failure {
	if (this is CancellationException) throw this
	return when (this) {
		is HttpException -> ResultState.Failure(parseHttpErrorMessage(this), this)
		is IOException -> {
			Log.w(tag, "Network error: $this")
			ResultState.Failure(ApiMessages.NETWORK_ERROR, this)
		}
		is JsonParseException -> {
			// The server answered, but not in the shape the app expects.
			Log.e(tag, "Failed to parse response", this)
			ResultState.Failure(ApiMessages.GENERIC_ERROR, this)
		}
		else -> {
			Log.e(tag, "Unexpected error", this)
			ResultState.Failure(ApiMessages.GENERIC_ERROR, this)
		}
	}
}

private fun parseHttpErrorMessage(e: HttpException): String {
	val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
	return runCatching {
		JSONObject(errorBody.orEmpty()).optString("message").takeIf { it.isNotBlank() }
	}.getOrNull() ?: ApiMessages.GENERIC_ERROR
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

/**
 * For action endpoints (like, follow, register, ...) whose payload is only a status string.
 * A successful HTTP response with `"data": null` is still a success; the server [BaseResponse.message]
 * stands in for the missing payload.
 */
fun flowFromMessageApi(
	apiCall: suspend () -> BaseResponse<String>
): Flow<ResultState<String>> = flow {
	emit(ResultState.Loading)

	when (val result = safeApiCall { apiCall() }) {
		is ResultState.Success -> emit(ResultState.Success(result.data.data ?: result.data.message))
		is ResultState.Failure -> emit(result)
		else -> Unit
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
	} catch (e: Exception) {
		if (e is HttpException && e.code() == 404) {
			emit(ResultState.Success(emptyValue))
		} else {
			emit(e.toFailure("flowFromApiOrEmptyOn404"))
		}
	}
}
