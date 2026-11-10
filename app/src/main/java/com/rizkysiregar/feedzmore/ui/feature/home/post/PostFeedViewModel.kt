package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizkysiregar.feedzmore.domain.post.MediaPreparationUseCase
import com.rizkysiregar.feedzmore.domain.post.UploadManager
import com.rizkysiregar.feedzmore.media.MediaValidationException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 06/12/25
 * Description: Software Engineer
 */

@HiltViewModel
class PostFeedViewModel @Inject constructor(
	private val app: Application,
	private val mediaPreparationUseCase: MediaPreparationUseCase,
	private val uploadManager: UploadManager
) : ViewModel() {

	// UI State for watching property changes
	private val _uiState = MutableStateFlow(PostFeedUiState())
	val uiState = _uiState.asStateFlow()

	val isPreparing: StateFlow<Boolean> = mediaPreparationUseCase.isPreparing

	private val _navigateHome = MutableSharedFlow<Unit>()
	val navigateHome: SharedFlow<Unit> = _navigateHome.asSharedFlow()

	fun onImageSelected(uri: Uri) {
		Log.i("PostFeedViewModel", "## onImageSelected() called")
		val mimeType = app.contentResolver.getType(uri)
		val error = validateMedia(uri, mimeType)
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

	/**
	 * Prepares the media (compress/copy) on this screen, then hands the upload
	 * off to the background worker and signals the UI to return home.
	 */
	fun submitPost() {
		val state = _uiState.value
		val mediaUri = state.selectedImage ?: return
		if (state.validationError != null) return

		viewModelScope.launch {
			try {
				val prepared = mediaPreparationUseCase.prepare(mediaUri, state.selectedMimeType)
				uploadManager.enqueue(
					filePath = prepared.filePath,
					contentType = prepared.contentType,
					caption = state.caption
				)
				_navigateHome.emit(Unit)
			} catch (e: MediaValidationException) {
				_uiState.update { it.copy(validationError = e.message ?: "Media tidak valid") }
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				_uiState.update { it.copy(validationError = "Gagal menyiapkan media, silakan coba lagi") }
			}
		}
	}

	private fun validateMedia(uri: Uri, mimeType: String?): String? {
		if (mimeType == null) return "Tipe media tidak dikenali"
		val sizeBytes = runCatching {
			app.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
		}.getOrNull()
		return when {
			mimeType.startsWith("image/") -> {
				when {
					mimeType !in ALLOWED_IMAGE_TYPES -> "Format gambar tidak didukung"
					// Static images are compressed before upload; GIFs are sent as-is.
					mimeType == "image/gif" && sizeBytes != null && sizeBytes > 5L * 1024 * 1024 ->
						"Ukuran GIF maksimal 5MB"
					else -> null
				}
			}
			mimeType in ALLOWED_VIDEO_TYPES -> {
				if (sizeBytes != null && sizeBytes > 50L * 1024 * 1024) return "Ukuran video maksimal 50MB"
				val durationMs = runCatching {
					val retriever = android.media.MediaMetadataRetriever()
					app.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
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

	fun clearValidationError() {
		_uiState.update { it.copy(validationError = null) }
	}

	fun onCaptionChanged(caption: String) {
		Log.i("PostFeedViewModel", "## onCaptionChanged() called")
		_uiState.update {
			it.copy(caption = caption)
		}
	}

	private companion object {
		const val RECENT_IMAGE_LIMIT = 18
		val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
		val ALLOWED_VIDEO_TYPES = setOf("video/mp4", "video/quicktime", "video/webm")
	}
}
