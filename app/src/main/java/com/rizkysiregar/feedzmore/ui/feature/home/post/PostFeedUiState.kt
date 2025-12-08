package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.net.Uri

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/12/25
 * Description: Software Engineer
 */
data class PostFeedUiState(
	val selectedImage: Uri? = null,
	val caption: String = ""
)
