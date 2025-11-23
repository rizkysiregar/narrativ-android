package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.runtime.Composable
import com.rizkysiregar.feedzmore.data.remote.model.response.FeedContentItem

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 23/11/25
 * Description: Software Engineer
 */

@Composable
fun FeedCardItem(
	item: FeedContentItem,
	onLikeClick: (FeedContentItem) -> Unit = {},
	onCommentClick: (FeedContentItem) -> Unit = {},
	onUserClick: (FeedContentItem) -> Unit = {}
) {

}


