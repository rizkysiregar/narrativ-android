package com.rizkysiregar.feedzmore.ui.feature.home.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.rizkysiregar.feedzmore.domain.post.PendingUpload
import com.rizkysiregar.feedzmore.domain.post.UploadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Tracks background uploads to show the top progress banner on Home
 * and bubbles successful/failed completions into the feed refresh trigger.
 */
@HiltViewModel
class UploadBannerViewModel @Inject constructor(
	private val uploadManager: UploadManager
) : ViewModel() {

	val pendingUploads = uploadManager.pendingUploads

	private val _feedRefreshVersion = MutableSharedFlow<Int>(extraBufferCapacity = 4)
	val feedRefreshVersion: SharedFlow<Int> = _feedRefreshVersion.asSharedFlow()

	init {
		viewModelScope.launch {
			uploadManager.completions.collect { _feedRefreshVersion.emit(System.currentTimeMillis().toInt()) }
		}
	}

	fun cancelUpload(id: UUID) {
		uploadManager.cancel(id)
	}
}