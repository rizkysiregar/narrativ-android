package com.rizkysiregar.narrativ.domain.post

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import java.util.UUID

/** Enqueues background post uploads and exposes their live state. */
interface UploadManager {
	/** Emits once for every upload that finished. */
	val completions: SharedFlow<Unit>

	/** The uploads that have not finished yet. */
	val pendingUploads: Flow<List<PendingUpload>>

	/** Enqueues one post: its [media] (0..5, in display order) and the [caption]; at least one of them is required. */
	fun enqueue(media: List<PreparedMedia>, caption: String)

	fun cancel(workId: UUID)

	fun notifyUploadCompleted()
}
