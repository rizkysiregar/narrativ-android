package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rizkysiregar.feedzmore.data.remote.model.response.FeedContentItem


/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FeedScreen(
	items: List<FeedContentItem>,
	isRefreshing: Boolean,
	onRefresh: () -> Unit,
	listState: LazyListState,
	isLoadingMore: Boolean,
) {

	val pullRefreshState = rememberPullRefreshState(
		refreshing = isRefreshing,
		onRefresh = onRefresh
	)

	Box(
		modifier = Modifier
			.fillMaxSize()
			.pullRefresh(pullRefreshState)
	) {
		LazyColumn(state = listState) {
			items(items) { item ->
				// card item
			}

			if (isLoadingMore) {
				item {
					// load more loading indicator
				}
			}
		}

		PullRefreshIndicator(
			refreshing = isRefreshing,
			state = pullRefreshState,
			modifier = Modifier
				.align(Alignment.TopCenter)
		)

	}
}