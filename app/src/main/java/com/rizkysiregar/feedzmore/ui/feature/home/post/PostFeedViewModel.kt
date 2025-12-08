package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 06/12/25
 * Description: Software Engineer
 */

@HiltViewModel
class PostFeedViewModel @Inject constructor(
	private val app: Application
) : ViewModel() {


//	private val _selectedImageUri = MutableStateFlow<Uri?>(null)
//	val selectedImageUri = _selectedImageUri.asStateFlow()

	private val _uiState = MutableStateFlow(PostFeedUiState())
	val uiState = _uiState.asStateFlow()


	fun onImageSelected(uri: Uri) {
		Log.i("PostFeedViewModel", "## onImageSelected() called")
		_uiState.update {
			it.copy(selectedImage = uri)
		}
	}

	fun onCaptionChanged(caption: String) {
		Log.i("PostFeedViewModel", "## onCaptionChanged() called")
		_uiState.update {
			it.copy(caption = caption)
		}
	}


	/*	private fun loadGalleryPhotos() {
			viewModelScope.launch(Dispatchers.IO) {
				val images = mutableListOf<String>()

				val projection = arrayOf<String>(
					MediaStore.Images.Media._ID,
					MediaStore.Images.Media.DISPLAY_NAME
				)

				val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

				val query = app.contentResolver.query(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					projection,
					null,
					null,
					sortOrder
				)

				query?.use { cursor ->
					val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
					while (cursor.moveToNext()) {
						val id = cursor.getLong(idColumn)
						val uri = "${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}/$id"
						images.add(uri)
					}
				}

				println("## images size: " + images.size)
				_uiState.update {
					it.copy(galleryImage = images)
				}
			}
		}




	 */
}


