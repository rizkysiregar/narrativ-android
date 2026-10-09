package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.request.feed.PostingFeedRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.PostingFeedResponse
import com.rizkysiregar.feedzmore.data.repository.UserDataRepository
import com.rizkysiregar.feedzmore.domain.feed.PostingFeedUseCase
import com.rizkysiregar.feedzmore.util.uriToRequestBody
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 06/12/25
 * Description: Software Engineer
 */

@HiltViewModel
class PostFeedViewModel @Inject constructor(
	private val app: Application,
	private val postingFeedUseCase: PostingFeedUseCase,
	private val userDataRepository: UserDataRepository
) : ViewModel() {


//	private val _selectedImageUri = MutableStateFlow<Uri?>(null)
//	val selectedImageUri = _selectedImageUri.asStateFlow()


	// UI State for watching property changes
	private val _uiState = MutableStateFlow(PostFeedUiState())
	val uiState = _uiState.asStateFlow()


	// State of posting, whether is loading, success or failure
	private val _postingState = MutableStateFlow<ResultState<PostingFeedResponse>?>(null)
	val postingState: StateFlow<ResultState<PostingFeedResponse>?> = _postingState
	fun onImageSelected(uri: Uri) {
		Log.i("PostFeedViewModel", "## onImageSelected() called")
		val mimeType = app.contentResolver.getType(uri)
		val error = validateMedia(app, uri, mimeType)
		_uiState.update {
			if (error != null) {
				it.copy(validationError = error, selectedImage = null, selectedMimeType = null)
			} else {
				it.copy(
					selectedImage = uri,
					selectedMimeType = mimeType,
					validationError = null
				)
			}
		}
	}

	private fun validateMedia(context: Application, uri: Uri, mimeType: String?): String? {
		if (mimeType == null) return "Tipe media tidak dikenali"
		val sizeBytes = runCatching {
			context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
		}.getOrNull()
		return when {
			mimeType.startsWith("image/") -> {
				if (sizeBytes != null && sizeBytes > 5L * 1024 * 1024) "Ukuran gambar maksimal 5MB"
				else null
			}
			mimeType == "video/mp4" || mimeType == "video/quicktime" || mimeType == "video/webm" -> {
				if (sizeBytes != null && sizeBytes > 50L * 1024 * 1024) return "Ukuran video maksimal 50MB"
				val durationMs = runCatching {
					val retriever = android.media.MediaMetadataRetriever()
					context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
						retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
					}
					val d = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
					retriever.release()
					d?.toLongOrNull()
				}.getOrNull()
				if (durationMs != null && durationMs > 60_000L) "Durasi video maksimal 60 detik"
				else null
			}
			else -> "Format media tidak didukung. Gunakan gambar atau video mp4/mov/webm"
		}
	}

	fun loadRecentImages() {
		if (_uiState.value.isLoadingGallery) return

		viewModelScope.launch(Dispatchers.IO) {
			_uiState.update { it.copy(isLoadingGallery = true) }

			val images = runCatching {
				val projection = arrayOf(MediaStore.Images.Media._ID)
				app.contentResolver.query(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					projection,
					null,
					null,
					"${MediaStore.Images.Media.DATE_ADDED} DESC"
				)?.use { cursor ->
					val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
					buildList {
						while (cursor.moveToNext() && size < RECENT_IMAGE_LIMIT) {
							add(
								ContentUris.withAppendedId(
									MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
									cursor.getLong(idColumn)
								)
							)
						}
					}
				} ?: emptyList()
			}.getOrDefault(emptyList())

			_uiState.update { state ->
				state.copy(
					recentImages = images,
					selectedImage = state.selectedImage ?: images.firstOrNull(),
					isLoadingGallery = false
				)
			}
		}
	}

	fun onCaptionChanged(caption: String) {
		Log.i("PostFeedViewModel", "## onCaptionChanged() called")
		_uiState.update {
			it.copy(caption = caption)
		}
	}

	fun postingFeed(postingFeedRequest: PostingFeedRequest) {
		viewModelScope.launch {
			val currentUsername = userDataRepository.username.first() { it.isNotEmpty() }

		// -- Convert Media URI to MultipartBody.Part -- //
		val mimeType = app.contentResolver.getType(postingFeedRequest.mediaUri)
		val mediaRequestBody =
			uriToRequestBody(app.applicationContext, postingFeedRequest.mediaUri, mimeType)

		val extension = mimeToExtension(mimeType)
		val mediaPart = MultipartBody.Part.createFormData(
			name = "media",
			filename = "upload_media.$extension",
			body = mediaRequestBody
		)

		postingFeedRequest.mediaPart = mediaPart
		postingFeedRequest.username = currentUsername

			// -- Invoke UseCase -- //
			postingFeedUseCase(postingFeedRequest).collect { result ->
				println("## postingState: $result")
				_postingState.value = result
			}
		}
	}

	fun resetPostingState() {
		_postingState.value = ResultState.Idle
	}

	private fun mimeToExtension(mimeType: String?): String = when (mimeType) {
		"video/mp4" -> "mp4"
		"video/quicktime" -> "mov"
		"video/webm" -> "webm"
		"image/png" -> "png"
		"image/webp" -> "webp"
		"image/gif" -> "gif"
		else -> if (mimeType?.startsWith("video/") == true) "mp4" else "jpg"
	}

	private companion object {
		const val RECENT_IMAGE_LIMIT = 18
	}
}

