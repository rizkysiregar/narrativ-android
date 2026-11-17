package com.rizkysiregar.narrativ.domain.post

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rizkysiregar.narrativ.data.remote.upload.PostUploadWorker
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
import com.rizkysiregar.narrativ.i18n.AppStrings
import com.rizkysiregar.narrativ.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Enqueues background post uploads and exposes their live state.
 * WorkManager is the single source of truth, so uploads survive navigation and
 * process death.
 */
@Singleton
class UploadManager @Inject constructor(
	@ApplicationContext context: Context,
	private val appStrings: AppStrings
) {

	private val workManager = WorkManager.getInstance(context)

	private val _completions = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
	val completions: SharedFlow<Unit> = _completions.asSharedFlow()

	val pendingUploads: Flow<List<PendingUpload>> =
		workManager.getWorkInfosByTagFlow(TAG).map { infos ->
			infos.filter { !it.state.isFinished }
				.mapNotNull { it.toPendingUpload() }
		}

	/** Enqueues one post: its [media] (0..5, in display order) and the [caption]; at least one of them is required. */
	fun enqueue(media: List<PreparedMedia>, caption: String) {
		Log.i("PostFlow", "enqueue: media=${media.size} captionLength=${caption.length}")
		require(media.isNotEmpty() || caption.isNotBlank()) { "A post needs a caption or a media" }
		val request = OneTimeWorkRequestBuilder<PostUploadWorker>()
			.setInputData(
				workDataOf(
					PostUploadWorker.KEY_FILE_PATHS to media.map { it.filePath }.toTypedArray(),
					PostUploadWorker.KEY_CONTENT_TYPES to media.map { it.contentType }.toTypedArray(),
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

		val name = uniqueName(media, caption)
		workManager.enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, request)
		Log.i("PostFlow", "enqueue: work $name handed to WorkManager (id=${request.id})")
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
			stageLabel = progress.getString(PostUploadWorker.KEY_STAGE) ?: appStrings.get(R.string.stage_waiting),
			progress = progress.getInt(PostUploadWorker.KEY_PERCENT, 0),
			state = state
		)
	}

	companion object {
		const val TAG = "post_upload"
		private const val CAPTION_TAG_PREFIX = "caption:"
		private const val BACKOFF_SECONDS = 10L

		private fun captionTag(caption: String) = CAPTION_TAG_PREFIX + caption

		// a caption-only post has no file to tell two posts apart, so a fresh id keeps them from replacing each other
		private fun uniqueName(media: List<PreparedMedia>, caption: String) =
			"upload_" + if (media.isEmpty()) "${caption.hashCode()}_${UUID.randomUUID()}" else media.joinToString("|") { it.filePath }.hashCode()
	}
}
