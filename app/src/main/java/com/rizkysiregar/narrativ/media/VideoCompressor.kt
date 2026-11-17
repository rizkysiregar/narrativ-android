package com.rizkysiregar.narrativ.media

import android.net.Uri
import java.io.File

/**
 * Transcodes a selected video into an H.264/AAC MP4 that fits within the backend size cap, targeting a bitrate
 * derived from the clip duration.
 */
interface VideoCompressor {
	/** Throws [MediaValidationException] when the video is too long or cannot be read, or processing fails. */
	suspend fun compress(inputUri: Uri): File
}
