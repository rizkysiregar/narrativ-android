package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FeedScreen(
	uiState: FeedUiState,
	onRefresh: () -> Unit,
	onRetry: () -> Unit,
	listState: LazyListState = rememberLazyListState()
) {
	val pullState =
		rememberPullRefreshState(refreshing = uiState.isRefreshing, onRefresh = onRefresh)

	Box(
		modifier = Modifier
			.fillMaxSize()
			.pullRefresh(pullState)
	) {
		LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
			items(uiState.items, key = { it.id }) { item ->
				FeedItemCard(
					item = item,
					onLikeClick = {},
					onCommentClick = { }
				)
			}

			if (uiState.isLoadingMore) {
				item {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.padding(16.dp),
						contentAlignment = Alignment.Center
					) {
						CircularProgressIndicator()
					}
				}
			}

			if (uiState.errorMessage != null && uiState.items.isEmpty()) {
				// full screen error with retry if initial load failed
				item {
					Column(
						modifier = Modifier
							.fillMaxSize()
							.padding(24.dp),
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						Text(text = uiState.errorMessage)
						Spacer(modifier = Modifier.height(8.dp))
						Button(onClick = onRetry) { Text("Retry") }
					}
				}
			}
		}

		PullRefreshIndicator(
			refreshing = uiState.isRefreshing,
			state = pullState,
			modifier = Modifier.align(Alignment.TopCenter)
		)
	}
}
