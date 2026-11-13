package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.net.Uri
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
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
	val caption: String = "",
	val validationError: String? = null,
	// the signed in user, shown next to the caption; null until the profile is loaded
	val author: UserSearchItemResponse? = null
) {
	/** Anything the user would lose by leaving the screen. */
	val hasContent: Boolean
		get() = selectedMedia.isNotEmpty() || caption.isNotBlank()


	/** A post needs a caption or at least one media. */
	val canSubmit: Boolean
		get() = (selectedMedia.isNotEmpty() || caption.isNotBlank()) && caption.length <= MAX_CAPTION_LENGTH
}
