package com.rizkysiregar.narrativ.domain.post

import android.net.Uri
import com.rizkysiregar.narrativ.media.MediaValidationException
import kotlinx.coroutines.flow.StateFlow

/**
 * Prepares the picked media into a stable local file before the background upload runs: videos are transcoded,
 * images are copied to cache.
 */
interface MediaPreparationUseCase {
	val isPreparing: StateFlow<Boolean>

	/**
	 * Copies/compresses the selected media into app cache and returns the local
	 * file plus the exact content type to request the presigned URL with.
	 * Throws [MediaValidationException] when the media cannot be accepted.
	 */
	suspend fun prepare(uri: Uri, mimeType: String?): PreparedMedia

	/**
	 * Prepares every picked media in order, keeping [isPreparing] on for the whole batch. When one of them
	 * is rejected, the files already prepared are deleted so nothing is left behind in the cache.
	 */
	suspend fun prepareAll(items: List<PickedMedia>): List<PreparedMedia>
}

/**
 * Local file ready to be uploaded, with the content type that must be used both
 * for the presigned URL request and the storage PUT (it is signed).
 */
data class PreparedMedia(
	val filePath: String,
	val contentType: String
)

/** A media the user picked, before it is compressed or copied. */
data class PickedMedia(
	val uri: Uri,
	val mimeType: String?
)
