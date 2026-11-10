package com.rizkysiregar.feedzmore.data.remote.upload

import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.rizkysiregar.feedzmore.data.repository.PostRepository
import com.rizkysiregar.feedzmore.domain.post.UploadManager
import com.rizkysiregar.feedzmore.notification.UploadNotifications
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.io.IOException

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Runs the presigned upload flow in the background: for every media of the
 * post (zero to five, in order) request an upload URL and PUT the file to storage, then
 * finalize the post with the object keys. A caption-only post skips the uploads. Reports
 * progress through WorkManager and a foreground notification, retrying transient failures.
 */
@HiltWorker
class PostUploadWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val postRepository: PostRepository,
	private val presignedUploader: PresignedUploader,
	private val uploadManager: UploadManager
) : CoroutineWorker(appContext, params) {

	override suspend fun getForegroundInfo(): ForegroundInfo =
		ForegroundInfo(
			progressNotificationId(),
			UploadNotifications.buildProgress(
				context = applicationContext,
				caption = inputData.getString(KEY_CAPTION).orEmpty(),
				stageLabel = STAGE_REQUESTING,
				percent = 0
			),
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
		)

	override suspend fun doWork(): Result {
		val filePaths = inputData.getStringArray(KEY_FILE_PATHS).orEmpty()
		val contentTypes = inputData.getStringArray(KEY_CONTENT_TYPES).orEmpty()
		val caption = inputData.getString(KEY_CAPTION).orEmpty()
		Log.i(TAG, "doWork: files=${filePaths.size} contentTypes=${contentTypes.size} captionLength=${caption.length} attempt=$runAttemptCount")

		if (filePaths.size != contentTypes.size) {
			Log.w(TAG, "doWork: stop, file and content type counts differ")
			return Result.failure(errorData("Tipe media tidak dikenal"))
		}
		if (filePaths.isEmpty() && caption.isBlank()) {
			Log.w(TAG, "doWork: stop, no media and no caption")
			return Result.failure(errorData("Post tidak boleh kosong"))
		}
		val files = filePaths.map(::File)
		if (files.any { !it.exists() }) {
			Log.w(TAG, "doWork: stop, a media file is missing")
			return Result.failure(errorData("File media tidak ditemukan"))
		}

		val progress = MutableStateFlow(UploadProgress(STAGE_UPLOADING, 0))
		val progressScope = CoroutineScope(Dispatchers.Default)
		val progressJob = progressScope.launch {
			progress.collect { publishProgress(caption, it.label, it.percent) }
		}

		return try {
			// the foreground notification is a nicety: when the system refuses it the upload must still go on
			try {
				setForeground(getForegroundInfo())
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				Log.w(TAG, "doWork: could not move to the foreground, uploading in the background", e)
			}
			publishProgress(caption, STAGE_REQUESTING, 0)

			val objectKeys = files.mapIndexed { index, file ->
				val label = uploadingLabel(index, files.size)
				val contentType = contentTypes[index]
				progress.value = UploadProgress(label, overallPercent(index, files.size, 0f))
				uploadOne(file, contentType, caption) { fraction ->
					progress.value = UploadProgress(label, overallPercent(index, files.size, fraction))
				}
			}

			publishProgress(caption, STAGE_FINALIZING, 100)
			Log.i(TAG, "doWork: calling POST /posts with ${objectKeys.size} media keys")
			postRepository.createPost(caption, objectKeys)
			Log.i(TAG, "doWork: POST /posts done")

			files.forEach { it.delete() }
			notifyResult(caption, success = true)
			uploadManager.notifyUploadCompleted()
			Result.success()
		} catch (e: HttpException) {
			Log.w(TAG, "doWork: server answered ${e.code()}")
			handleHttpError(e, caption)
		} catch (e: UploadHttpException) {
			notifyResult(caption, success = false)
			Result.failure(errorData("Upload media gagal (${e.code})"))
		} catch (e: IOException) {
			if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
				publishProgress(caption, STAGE_RETRYING, 0)
				Result.retry()
			} else {
				notifyResult(caption, success = false)
				Result.failure(errorData("Koneksi bermasalah saat mengunggah"))
			}
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			Log.e(TAG, "doWork failed", e)
			notifyResult(caption, success = false)
			Result.failure(errorData(e.message ?: "Upload gagal"))
		} finally {
			progressJob.cancel()
		}
	}

	/** Requests an upload URL, PUTs the file (asking for a fresh URL once if it expired) and returns the object key. */
	private suspend fun uploadOne(
		file: File,
		contentType: String,
		caption: String,
		onProgress: (Float) -> Unit
	): String {
		var uploadUrl = postRepository.createUploadUrl(contentType)
		var refreshed = false
		while (true) {
			try {
				presignedUploader.upload(uploadUrl.presignedUrl, file, contentType, onProgress)
				return uploadUrl.objectKey
			} catch (e: UploadHttpException) {
				if (e.code == 403 && !refreshed) {
					refreshed = true
					publishProgress(caption, STAGE_REQUESTING, 0)
					uploadUrl = postRepository.createUploadUrl(contentType)
					continue
				}
				throw e
			}
		}
	}

	// every file gets an equal share of 0..99; 100 is reserved for finalizing the post
	private fun overallPercent(index: Int, total: Int, fraction: Float): Int =
		(((index + fraction.coerceIn(0f, 1f)) / total) * 99).toInt().coerceIn(0, 99)

	private fun uploadingLabel(index: Int, total: Int): String =
		if (total > 1) "$STAGE_UPLOADING ${index + 1}/$total" else STAGE_UPLOADING

	private data class UploadProgress(val label: String, val percent: Int)

	private suspend fun handleHttpError(e: HttpException, caption: String): Result =
		when (e.code()) {
			in 500..599 -> if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
				publishProgress(caption, STAGE_RETRYING, 0)
				Result.retry()
			} else {
				notifyResult(caption, success = false)
				Result.failure(errorData("Server sedang bermasalah, coba lagi nanti"))
			}

			429 -> {
				notifyResult(caption, success = false)
				Result.failure(errorData("Terlalu banyak permintaan, coba lagi nanti"))
			}

			else -> {
				notifyResult(caption, success = false)
				Result.failure(errorData("Upload ditolak (${e.code()})"))
			}
		}

	private suspend fun publishProgress(caption: String, stage: String, percent: Int) {
		setProgress(
			workDataOf(
				KEY_CAPTION to caption,
				KEY_STAGE to stage,
				KEY_PERCENT to percent
			)
		)
		UploadNotifications.notifyProgress(
			context = applicationContext,
			notificationId = progressNotificationId(),
			caption = caption,
			stageLabel = stage,
			percent = percent
		)
	}

	private fun notifyResult(caption: String, success: Boolean) {
		UploadNotifications.notifyResult(
			context = applicationContext,
			notificationId = resultNotificationId(),
			caption = caption,
			success = success
		)
	}

	private fun progressNotificationId(): Int = id.hashCode()

	private fun resultNotificationId(): Int = id.hashCode() + 1

	private fun errorData(message: String) = workDataOf(KEY_ERROR to message)

	companion object {
		private const val TAG = "PostFlow"
		const val KEY_FILE_PATHS = "file_paths"
		const val KEY_CONTENT_TYPES = "content_types"
		const val KEY_CAPTION = "caption"
		const val KEY_STAGE = "stage"
		const val KEY_PERCENT = "percent"
		const val KEY_ERROR = "error"

		const val STAGE_REQUESTING = "Menyiapkan unggahan"
		const val STAGE_UPLOADING = "Mengunggah"
		const val STAGE_FINALIZING = "Menyelesaikan"
		const val STAGE_RETRYING = "Mencoba lagi"

		private const val MAX_RETRY_ATTEMPTS = 3
	}
}
