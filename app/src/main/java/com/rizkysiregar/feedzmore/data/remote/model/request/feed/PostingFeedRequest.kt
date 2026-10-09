package com.rizkysiregar.feedzmore.data.remote.model.request.feed

import android.net.Uri
import okhttp3.MultipartBody

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 09/12/25
 * Description: Software Engineer
 */
data class PostingFeedRequest(
	var username: String,
	val caption: String,
	val mediaUri: Uri,
	var mediaPart: MultipartBody.Part?
)