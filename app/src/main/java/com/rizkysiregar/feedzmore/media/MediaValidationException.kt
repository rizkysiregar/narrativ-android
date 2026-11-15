package com.rizkysiregar.feedzmore.media

import androidx.annotation.StringRes

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Raised when selected media cannot be prepared for upload.
 */
class MediaValidationException(
	@StringRes val messageRes: Int,
	val args: List<Any> = emptyList()
) : Exception("Media rejected (string resource $messageRes)")
