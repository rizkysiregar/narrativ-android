package com.rizkysiregar.narrativ.media

import java.io.File

/**
 * Shrinks a picked image to fit the backend size cap while keeping it visually sharp: downscales to a feed-friendly
 * resolution, applies EXIF rotation, then re-encodes with the highest quality that stays under the target size.
 * Images that are already small enough are returned untouched.
 */
interface ImageCompressor {
	/**
	 * Returns the file to upload and its content type. [source] is a private cache
	 * copy of the picked image; it is deleted when a new file is produced.
	 * Throws [MediaValidationException] when the image cannot be read or cannot be made small enough.
	 */
	suspend fun compress(source: File, mimeType: String): CompressedImage

	/**
	 * A profile picture is shown small, so it is always re-encoded (which also applies the EXIF rotation)
	 * at a low resolution to stay well under the backend's 2MB cap.
	 */
	suspend fun compressAvatar(source: File, mimeType: String): CompressedImage
}

/** Image ready to upload, with the content type matching its encoding. */
data class CompressedImage(
	val file: File,
	val contentType: String
)
