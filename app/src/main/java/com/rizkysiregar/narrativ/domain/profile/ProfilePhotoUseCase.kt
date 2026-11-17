package com.rizkysiregar.narrativ.domain.profile

import android.content.Context
import android.net.Uri
import com.rizkysiregar.narrativ.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.narrativ.data.remote.upload.PresignedUploader
import com.rizkysiregar.narrativ.data.repository.UserRepository
import com.rizkysiregar.narrativ.media.ImageCompressor
import com.rizkysiregar.narrativ.media.MediaValidationException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import com.rizkysiregar.narrativ.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 11/11/26
 * Description: Changes or removes the signed-in user's profile picture. A picked image is copied to
 * cache, shrunk, uploaded straight to storage through a presigned PUT and then confirmed to the
 * backend, which returns the refreshed profile. Both calls throw when anything fails.
 */
class ProfilePhotoUseCase @Inject constructor(
	@ApplicationContext private val context: Context,
	private val userRepository: UserRepository,
	private val imageCompressor: ImageCompressor,
	private val uploader: PresignedUploader
) {

	suspend fun upload(
		uri: Uri,
		mimeType: String?,
		onProgress: (Float) -> Unit = {}
	): UserSearchItemResponse {
		val resolvedMime = mimeType ?: context.contentResolver.getType(uri)
		if (resolvedMime !in ALLOWED_TYPES) {
			throw MediaValidationException(R.string.media_err_photo_format)
		}

		val copied = withContext(Dispatchers.IO) { copyToCache(uri) }
		val image = imageCompressor.compressAvatar(copied, resolvedMime!!)
		try {
			val target = userRepository.createProfileImageUploadUrl(image.contentType)
			uploader.upload(target.presignedUrl, image.file, image.contentType, onProgress)
			return userRepository.setProfileImage(target.objectKey)
		} finally {
			withContext(Dispatchers.IO) { image.file.delete() }
		}
	}

	suspend fun remove(): UserSearchItemResponse = userRepository.deleteProfileImage()

	private fun copyToCache(uri: Uri): File {
		val file = File(context.cacheDir, "avatar_upload_${UUID.randomUUID()}")
		context.contentResolver.openInputStream(uri)?.use { input ->
			file.outputStream().use { output -> input.copyTo(output) }
		} ?: throw MediaValidationException(R.string.media_err_photo_read)
		return file
	}

	private companion object {
		val ALLOWED_TYPES = setOf("image/jpeg", "image/png", "image/webp")
	}
}
