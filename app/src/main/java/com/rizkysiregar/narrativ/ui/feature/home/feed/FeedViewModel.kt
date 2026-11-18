package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.lifecycle.SavedStateHandle
import androidx.media3.exoplayer.ExoPlayer
import com.rizkysiregar.narrativ.data.cache.DataStoreManager
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.request.feed.GetAllFeedRequest
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedsPostPaginationResponse
import com.rizkysiregar.narrativ.domain.feed.DislikePostUseCase
import com.rizkysiregar.narrativ.domain.feed.GetAllFeedUseCase
import com.rizkysiregar.narrativ.domain.feed.LikePostUseCase
import com.rizkysiregar.narrativ.media.VideoPlayerFactory
import com.rizkysiregar.narrativ.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getAllFeedUseCase: GetAllFeedUseCase,
    dislikePostUseCase: DislikePostUseCase,
    likePostUseCase: LikePostUseCase,
    dataStore: DataStoreManager,
    private val playerFactory: VideoPlayerFactory
) : BaseFeedViewModel(dataStore, dislikePostUseCase, likePostUseCase) {

    /**
     * Shared feed player owned by this ViewModel so it survives navigation to
     * PostDetail (the prepared item + buffer are kept, position resumes on back).
     */
    val player: ExoPlayer
        get() = _player ?: playerFactory.create().also { _player = it }

    private var _player: ExoPlayer? = null

    init {
        refresh()
    }

    override fun onCleared() {
        _player?.release()
        _player = null
        super.onCleared()
    }

    override suspend fun fetchPage(
        page: Int,
        size: Int,
        cursor: String?,
        mediaOnly: Boolean
    ): Flow<ResultState<FeedsPostPaginationResponse>> =
        // home is list only: the grid stream is never requested here
        getAllFeedUseCase(GetAllFeedRequest(page, size, cursor = cursor))

}
