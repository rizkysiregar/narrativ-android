package com.rizkysiregar.feedzmore.data.remote.upload

import com.rizkysiregar.feedzmore.di.UploadClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Buffer
import okio.source
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Streams a file to a presigned PUT URL. The Content-Type header is
 * sent exactly as requested from the backend because it is part of the signature.
 * Network failures are retried a limited number of times with linear backoff.
 */
@Singleton
class PresignedUploader @Inject constructor(
	@UploadClient private val client: OkHttpClient
) {

	suspend fun upload(
		url: String,
		file: File,
		contentType: String,
		onProgress: (Float) -> Unit
	) = withContext(Dispatchers.IO) {
		val request = Request.Builder()
			.url(url)
			.put(ProgressRequestBody(file, contentType.toMediaType(), onProgress))
			.header("Content-Type", contentType)
			.build()

		var attempt = 0
		while (true) {
			try {
				client.newCall(request).execute().use { response ->
					if (!response.isSuccessful) {
						throw UploadHttpException(
							code = response.code,
							retryAfterSeconds = response.header("Retry-After")?.toLongOrNull()
						)
					}
				}
				return@withContext
			} catch (e: IOException) {
				if (attempt >= MAX_RETRIES) throw e
				delay(RETRY_BASE_DELAY_MS * (attempt + 1))
				attempt++
			}
		}
	}

	private class ProgressRequestBody(
		private val file: File,
		private val mediaType: MediaType?,
		private val onProgress: (Float) -> Unit
	) : RequestBody() {

		override fun contentType(): MediaType? = mediaType

		override fun contentLength(): Long = file.length()

		override fun writeTo(sink: BufferedSink) {
			val total = file.length().coerceAtLeast(1L)
			file.source().use { source ->
				val buffer = Buffer()
				var written = 0L
				while (true) {
					val read = source.read(buffer, BUFFER_SIZE.toLong())
					if (read == -1L) break
					sink.write(buffer, read)
					written += read
					onProgress((written.toDouble() / total).toFloat().coerceIn(0f, 1f))
				}
			}
		}
	}

	private companion object {
		const val MAX_RETRIES = 2
		const val RETRY_BASE_DELAY_MS = 500L
		const val BUFFER_SIZE = 64 * 1024
	}
}
