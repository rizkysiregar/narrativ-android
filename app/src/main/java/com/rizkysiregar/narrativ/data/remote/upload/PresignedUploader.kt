package com.rizkysiregar.narrativ.data.remote.upload

import com.rizkysiregar.narrativ.data.remote.model.response.PresignedUploadForm
import com.rizkysiregar.narrativ.di.IoDispatcher
import com.rizkysiregar.narrativ.di.UploadClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
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
 * Description: Streams a file to object storage, with a presigned POST form when the backend hands one out
 * (storage then refuses a file above the signed size limit) or else to a presigned PUT URL. The content type is
 * sent exactly as requested from the backend because it is part of the signature.
 * Network failures are retried a limited number of times with linear backoff.
 */
@Singleton
class PresignedUploader @Inject constructor(
	@UploadClient private val client: OkHttpClient,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

	/** Uses [form] when the backend sent one, else PUTs to [presignedUrl]. */
	suspend fun upload(
		presignedUrl: String,
		form: PresignedUploadForm?,
		file: File,
		contentType: String,
		onProgress: (Float) -> Unit
	) {
		if (form != null) post(form, file, contentType, onProgress) else upload(presignedUrl, file, contentType, onProgress)
	}

	suspend fun upload(
		url: String,
		file: File,
		contentType: String,
		onProgress: (Float) -> Unit
	) = execute(
		Request.Builder()
			.url(url)
			.put(ProgressRequestBody(file, contentType.toMediaType(), onProgress))
			.header("Content-Type", contentType)
			.build()
	)

	/** The signed fields first, as they are, then the file: storage reads the policy before the content. */
	suspend fun post(
		form: PresignedUploadForm,
		file: File,
		contentType: String,
		onProgress: (Float) -> Unit
	) {
		val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
			form.fields.forEach { (name, value) -> addFormDataPart(name, value) }
			addFormDataPart("file", file.name, ProgressRequestBody(file, contentType.toMediaType(), onProgress))
		}.build()
		execute(Request.Builder().url(form.url).post(body).build())
	}

	private suspend fun execute(request: Request) = withContext(ioDispatcher) {
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
