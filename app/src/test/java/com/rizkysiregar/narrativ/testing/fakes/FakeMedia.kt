package com.rizkysiregar.narrativ.testing.fakes

import android.net.Uri
import com.rizkysiregar.narrativ.domain.post.MediaInspector
import com.rizkysiregar.narrativ.domain.post.MediaPreparationUseCase
import com.rizkysiregar.narrativ.domain.post.PendingUpload
import com.rizkysiregar.narrativ.domain.post.PickedMedia
import com.rizkysiregar.narrativ.domain.post.PreparedMedia
import com.rizkysiregar.narrativ.domain.post.UploadManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** Answers from a table instead of the content resolver. A uri nobody described has no type, size or duration. */
class FakeMediaInspector : MediaInspector {
	data class Info(val mimeType: String?, val sizeBytes: Long? = null, val durationMs: Long? = null)

	private val infos = mutableMapOf<Uri, Info>()

	fun describe(uri: Uri, mimeType: String?, sizeBytes: Long? = null, durationMs: Long? = null) {
		infos[uri] = Info(mimeType, sizeBytes, durationMs)
	}

	override fun mimeType(uri: Uri) = infos[uri]?.mimeType
	override fun sizeBytes(uri: Uri) = infos[uri]?.sizeBytes
	override fun durationMs(uri: Uri) = infos[uri]?.durationMs
}

class FakeMediaPreparation : MediaPreparationUseCase {
	private val _isPreparing = MutableStateFlow(false)
	override val isPreparing: StateFlow<Boolean> = _isPreparing.asStateFlow()

	val prepared = mutableListOf<List<PickedMedia>>()

	/** Throw to make preparing fail. */
	var onPrepareAll: (List<PickedMedia>) -> Unit = {}

	override suspend fun prepare(uri: Uri, mimeType: String?): PreparedMedia = PreparedMedia("/cache/${UUID.randomUUID()}", mimeType.orEmpty())

	override suspend fun prepareAll(items: List<PickedMedia>): List<PreparedMedia> {
		prepared += items
		onPrepareAll(items)
		return items.mapIndexed { i, item -> PreparedMedia("/cache/$i", item.mimeType.orEmpty()) }
	}
}

class FakeUploadManager : UploadManager {
	data class Enqueued(val media: List<PreparedMedia>, val caption: String)

	val enqueued = mutableListOf<Enqueued>()
	val cancelled = mutableListOf<UUID>()
	private val _completions = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
	val pending = MutableStateFlow<List<PendingUpload>>(emptyList())

	override val completions: SharedFlow<Unit> = _completions
	override val pendingUploads: Flow<List<PendingUpload>> = pending

	override fun enqueue(media: List<PreparedMedia>, caption: String) {
		enqueued += Enqueued(media, caption)
	}

	override fun cancel(workId: UUID) {
		cancelled += workId
	}

	override fun notifyUploadCompleted() {
		_completions.tryEmit(Unit)
	}
}
