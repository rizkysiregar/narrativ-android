package com.rizkysiregar.feedzmore.data.remote.upload

import android.content.Context
import android.content.pm.ServiceInfo
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
 * Description: Runs the presigned upload flow in the background:
 * request upload URL -> PUT to storage -> finalize post. Reports progress through
 * WorkManager and a foreground notification, retrying transient failures.
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
		val filePath = inputData.getString(KEY_FILE_PATH)
			?: return Result.failure(errorData("File media tidak ditemukan"))
		val contentType = inputData.getString(KEY_CONTENT_TYPE)
			?: return Result.failure(errorData("Tipe media tidak dikenal"))
		val caption = inputData.getString(KEY_CAPTION).orEmpty()

		val file = File(filePath)
		if (!file.exists()) return Result.failure(errorData("File media tidak ditemukan"))

		val progress = MutableStateFlow(0)
		val progressScope = CoroutineScope(Dispatchers.Default)
		val progressJob = progressScope.launch {
			progress.collect { percent -> publishProgress(caption, STAGE_UPLOADING, percent) }
		}

		return try {
			setForeground(getForegroundInfo())
			publishProgress(caption, STAGE_REQUESTING, 0)
			var uploadUrl = postRepository.createUploadUrl(contentType)

			var refreshed = false
			while (true) {
				try {
					presignedUploader.upload(uploadUrl.presignedUrl, file, contentType) { fraction ->
						progress.value = (fraction * 100).toInt().coerceIn(0, 99)
					}
					break
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

			publishProgress(caption, STAGE_FINALIZING, 100)
			postRepository.createPost(caption, uploadUrl.objectKey)

			file.delete()
			notifyResult(caption, success = true)
			uploadManager.notifyUploadCompleted()
			Result.success()
		} catch (e: HttpException) {
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
			notifyResult(caption, success = false)
			Result.failure(errorData(e.message ?: "Upload gagal"))
		} finally {
			progressJob.cancel()
		}
	}

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
		const val KEY_FILE_PATH = "file_path"
		const val KEY_CONTENT_TYPE = "content_type"
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
