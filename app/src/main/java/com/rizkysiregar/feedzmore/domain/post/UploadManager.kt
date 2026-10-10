package com.rizkysiregar.feedzmore.domain.post

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rizkysiregar.feedzmore.data.remote.upload.PostUploadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Enqueues background post uploads and exposes their live state.
 * WorkManager is the single source of truth, so uploads survive navigation and
 * process death.
 */
@Singleton
class UploadManager @Inject constructor(
	@ApplicationContext context: Context
) {

	private val workManager = WorkManager.getInstance(context)

	private val _completions = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
	val completions: SharedFlow<Unit> = _completions.asSharedFlow()

	val pendingUploads: Flow<List<PendingUpload>> =
		workManager.getWorkInfosByTagFlow(TAG).map { infos ->
			infos.filter { !it.state.isFinished }
				.mapNotNull { it.toPendingUpload() }
		}

	fun enqueue(filePath: String, contentType: String, caption: String) {
		val request = OneTimeWorkRequestBuilder<PostUploadWorker>()
			.setInputData(
				workDataOf(
					PostUploadWorker.KEY_FILE_PATH to filePath,
					PostUploadWorker.KEY_CONTENT_TYPE to contentType,
					PostUploadWorker.KEY_CAPTION to caption
				)
			)
			.addTag(TAG)
			.addTag(captionTag(caption))
			.setConstraints(
				Constraints.Builder()
					.setRequiredNetworkType(NetworkType.CONNECTED)
					.build()
			)
			.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
			.build()

		workManager.enqueueUniqueWork(uniqueName(filePath), ExistingWorkPolicy.KEEP, request)
	}

	fun cancel(workId: UUID) {
		workManager.cancelWorkById(workId)
	}

	fun notifyUploadCompleted() {
		_completions.tryEmit(Unit)
	}

	private fun WorkInfo.toPendingUpload(): PendingUpload {
		val caption = tags.firstOrNull { it.startsWith(CAPTION_TAG_PREFIX) }
			?.removePrefix(CAPTION_TAG_PREFIX)
			?: progress.getString(PostUploadWorker.KEY_CAPTION)
			?: ""

		return PendingUpload(
			id = id,
			caption = caption,
			stageLabel = progress.getString(PostUploadWorker.KEY_STAGE) ?: "Menunggu koneksi",
			progress = progress.getInt(PostUploadWorker.KEY_PERCENT, 0),
			state = state
		)
	}

	companion object {
		const val TAG = "post_upload"
		private const val CAPTION_TAG_PREFIX = "caption:"
		private const val BACKOFF_SECONDS = 10L

		private fun captionTag(caption: String) = CAPTION_TAG_PREFIX + caption

		private fun uniqueName(filePath: String) = "upload_" + filePath.hashCode()
	}
}
