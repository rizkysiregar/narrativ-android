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
		_uiState.update {
			it.copy(selectedImage = uri)
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

			// -- Convert Image URI to MultipartBody.Part -- //
			val imageRequestBody =
				uriToRequestBody(app.applicationContext, postingFeedRequest.imageUri)

			val imagePart = MultipartBody.Part.createFormData(
				name = "image",
				filename = "upload_image.jpg",
				body = imageRequestBody
			)

			postingFeedRequest.imagePart = imagePart
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

	private companion object {
		const val RECENT_IMAGE_LIMIT = 18
	}
}

