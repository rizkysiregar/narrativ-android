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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme


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
	listState: LazyListState = rememberLazyListState(),
	onLikeClick: (String) -> Unit
) {
	// feed state
	val currentOnLikeClick by androidx.compose.runtime.rememberUpdatedState(onLikeClick)
	val pullState =
		rememberPullRefreshState(refreshing = uiState.isRefreshing, onRefresh = onRefresh)

	var selectedPostId by remember { mutableStateOf<String?>(null) }
	var showCommentSheet by remember { mutableStateOf(false) }

	Box(
		modifier = Modifier
			.fillMaxSize()
			.pullRefresh(pullState)
	) {
		LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
			items(
				items = uiState.items,
				key = { it.id },
				contentType = { "feed_item" }
			) { item ->
				FeedItemCard(
					item = item,
					onLikeClick = { id -> currentOnLikeClick(id) },
					onCommentClick = { post ->
						selectedPostId = post.id
						showCommentSheet = true
					}
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

		if (showCommentSheet && selectedPostId != null) {
			CommentSectionSheet(
				postId = selectedPostId!!,
				onDismiss = { showCommentSheet = false }
			)
		}

		PullRefreshIndicator(
			refreshing = uiState.isRefreshing,
			state = pullState,
			modifier = Modifier.align(Alignment.TopCenter)
		)
	}
}

@Composable
@Preview(showBackground = true)
fun FeedScreenPreview() {
	FeedzmoreTheme {
		FeedScreen(
			uiState = FeedUiState(
				items = List(10) { index ->
					FeedContentItem(
						id = index.toString(),
						caption = "This is caption $index",
						commentCount = 1,
						likeCount = 1,
						liked = true,
						createdAt = "2023-11-25",
						imageUrl = "https://dummy-url",
						username = "John Doe"
					)
				}
			),
			onRefresh = {},
			onRetry = {},
			listState = rememberLazyListState(),
			onLikeClick = {}
		)
	}
}
