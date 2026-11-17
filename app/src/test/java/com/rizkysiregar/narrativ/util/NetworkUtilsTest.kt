package com.rizkysiregar.narrativ.util

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParseException
import com.rizkysiregar.narrativ.data.remote.ApiMessages
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.BaseResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class NetworkUtilsTest {

	private fun httpError(code: Int, body: String) =
		HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

	private fun <T> ok(data: T?, message: String = "OK") = BaseResponse(code = "200", message = message, data = data)

	// --- safeApiCall ---

	@Test
	fun aSuccessfulCallIsWrapped() = runTest {
		assertThat(safeApiCall { 42 }).isEqualTo(ResultState.Success(42))
	}

	@Test
	fun anHttpErrorShowsTheMessageOfTheServer() = runTest {
		val error = httpError(409, """{"message":"Username sudah dipakai"}""")

		val result = safeApiCall { throw error } as ResultState.Failure

		assertThat(result.message).isEqualTo("Username sudah dipakai")
		assertThat(result.throwable).isSameInstanceAs(error)
	}

	@Test
	fun anHttpErrorWithoutAUsableBodyFallsBackToTheGenericMessage() = runTest {
		listOf("", "not json", """{"message":""}""", """{"other":"x"}""").forEach { body ->
			val result = safeApiCall { throw httpError(500, body) } as ResultState.Failure

			assertThat(result.message).isEqualTo(ApiMessages.GENERIC_ERROR)
		}
	}

	@Test
	fun aNetworkFailureIsToldApartFromAServerError() = runTest {
		val error = IOException("timeout")

		val result = safeApiCall { throw error } as ResultState.Failure

		assertThat(result.message).isEqualTo(ApiMessages.NETWORK_ERROR)
		assertThat(result.throwable).isSameInstanceAs(error)
	}

	@Test
	fun aResponseInTheWrongShapeIsAGenericFailureThatKeepsTheCause() = runTest {
		val error = JsonParseException("expected object")

		val result = safeApiCall { throw error } as ResultState.Failure

		assertThat(result.message).isEqualTo(ApiMessages.GENERIC_ERROR)
		assertThat(result.throwable).isSameInstanceAs(error)
	}

	@Test
	fun anUnexpectedExceptionIsAGenericFailure() = runTest {
		val result = safeApiCall { throw IllegalStateException("boom") } as ResultState.Failure

		assertThat(result.message).isEqualTo(ApiMessages.GENERIC_ERROR)
	}

	@Test
	fun cancellationIsNeverSwallowed() = runTest {
		assertThrows(CancellationException::class.java) {
			kotlinx.coroutines.runBlocking { safeApiCall { throw CancellationException("left the screen") } }
		}
	}

	// --- flowFromApi ---

	@Test
	fun flowFromApiEmitsLoadingThenTheData() = runTest {
		val states = flowFromApi { ok("payload") }.toList()

		assertThat(states).containsExactly(ResultState.Loading, ResultState.Success("payload")).inOrder()
	}

	@Test
	fun flowFromApiTreatsMissingDataAsAFailureWithTheServerMessage() = runTest {
		val states = flowFromApi<String> { ok(null, message = "Post tidak ditemukan") }.toList()

		assertThat(states).containsExactly(ResultState.Loading, ResultState.Failure("Post tidak ditemukan")).inOrder()
	}

	@Test
	fun flowFromApiReportsAnHttpErrorAfterLoading() = runTest {
		val states = flowFromApi<String> { throw httpError(401, """{"message":"Password salah"}""") }.toList()

		assertThat(states).hasSize(2)
		assertThat(states[0]).isEqualTo(ResultState.Loading)
		assertThat((states[1] as ResultState.Failure).message).isEqualTo("Password salah")
	}

	// --- flowFromMessageApi ---

	@Test
	fun anActionWithoutPayloadStillSucceedsWithTheServerMessage() = runTest {
		val states = flowFromMessageApi { ok(null, message = "Followed") }.toList()

		assertThat(states).containsExactly(ResultState.Loading, ResultState.Success("Followed")).inOrder()
	}

	@Test
	fun anActionPayloadWinsOverTheMessage() = runTest {
		val states = flowFromMessageApi { ok("done", message = "OK") }.toList()

		assertThat(states.last()).isEqualTo(ResultState.Success("done"))
	}

	@Test
	fun anActionFailureIsReported() = runTest {
		val states = flowFromMessageApi { throw IOException() }.toList()

		assertThat((states.last() as ResultState.Failure).message).isEqualTo(ApiMessages.NETWORK_ERROR)
	}

	// --- flowFromApiOrEmptyOn404 ---

	@Test
	fun a404BecomesTheEmptyValue() = runTest {
		val states = flowFromApiOrEmptyOn404(emptyList<String>()) { throw httpError(404, "{}") }.toList()

		assertThat(states).containsExactly(ResultState.Loading, ResultState.Success(emptyList<String>())).inOrder()
	}

	@Test
	fun anotherHttpErrorIsStillAFailure() = runTest {
		val states = flowFromApiOrEmptyOn404(emptyList<String>()) { throw httpError(500, """{"message":"down"}""") }.toList()

		assertThat((states.last() as ResultState.Failure).message).isEqualTo("down")
	}

	@Test
	fun missingDataIsAFailureEvenWhenA404WouldBeEmpty() = runTest {
		val states = flowFromApiOrEmptyOn404(emptyList<String>()) { ok(null, message = "kosong?") }.toList()

		assertThat(states.last()).isEqualTo(ResultState.Failure("kosong?"))
	}
}
