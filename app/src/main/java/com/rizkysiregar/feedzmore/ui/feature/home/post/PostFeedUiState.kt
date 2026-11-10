package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.net.Uri
import com.rizkysiregar.feedzmore.domain.post.PickedMedia

/** The backend rejects posts with more media or a longer caption than this. */
const val MAX_POST_MEDIA = 5
const val MAX_CAPTION_LENGTH = 500

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/12/25
 * Description: Software Engineer
 */
data class PostFeedUiState(
	// 0..MAX_POST_MEDIA, in the order they will be shown; a post may also be caption only
	val selectedMedia: List<PickedMedia> = emptyList(),
	// which of the selected media the large preview shows
	val previewIndex: Int = 0,
	val caption: String = "",
	val recentImages: List<Uri> = emptyList(),
	val isLoadingGallery: Boolean = false,
	val validationError: String? = null
) {
	/** A post needs a caption or at least one media. */
	val canSubmit: Boolean
		get() = (selectedMedia.isNotEmpty() || caption.isNotBlank()) && caption.length <= MAX_CAPTION_LENGTH
}
