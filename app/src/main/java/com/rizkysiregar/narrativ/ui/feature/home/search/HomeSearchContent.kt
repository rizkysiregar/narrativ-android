package com.rizkysiregar.narrativ.ui.feature.home.search

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.ui.feature.home.feed.FeedItemCard
import com.rizkysiregar.narrativ.ui.feature.home.feed.FeedUiState
import kotlinx.coroutines.flow.filterNotNull

/** The kinds of results of a search on home. */
enum class SearchTab(@StringRes val labelRes: Int) {
	TOP(R.string.search_tab_top),
	PEOPLE(R.string.search_tab_people),
	POSTS(R.string.search_tab_posts)
}

// the people shown above the posts on the top tab; the people tab lists them all
private const val TOP_PEOPLE = 3
private const val LOAD_AHEAD = 3

/**
 * The search on home, in place of the feed while it is open: the field, then the results in three tabs. Top shows the
 * best matching people above the matching posts, the other two tabs one kind each. All tabs read the same two
 * searches, so switching tabs asks the server for nothing new.
 */
@Composable
fun HomeSearchContent(
	people: SearchUserUiState,
	posts: FeedUiState,
	tab: SearchTab,
	onTabChange: (SearchTab) -> Unit,
	onQueryChange: (String) -> Unit,
	onClose: () -> Unit,
	onRetryPeople: () -> Unit,
	onLoadMorePeople: () -> Unit,
	onRetryPosts: () -> Unit,
	onLoadMorePosts: () -> Unit,
	onUserClick: (userId: String, username: String) -> Unit,
	onLikeClick: (String) -> Unit,
	onPostClick: (FeedContentItem) -> Unit,
	onCommentClick: (FeedContentItem) -> Unit,
	onMediaClick: (FeedContentItem, page: Int) -> Unit,
	onAuthorBlocked: () -> Unit,
	modifier: Modifier = Modifier
) {
	val focusRequester = remember { FocusRequester() }
	// opening the search is asking to type: the keyboard comes up at once
	LaunchedEffect(Unit) { focusRequester.requestFocus() }

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
			.imePadding()
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			IconButton(onClick = onClose) {
				Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
			}
			SearchUserBar(
				query = people.query,
				onQueryChange = onQueryChange,
				modifier = Modifier
					.weight(1f)
					.focusRequester(focusRequester)
			)
		}

		if (people.isQueryTooShort) {
			SearchHint()
			return@Column
		}

		SearchTabs(selected = tab, onSelect = onTabChange)
		HorizontalDivider()

		Box(modifier = Modifier.weight(1f)) {
			when (tab) {
				SearchTab.TOP -> TopResults(
					people = people,
					posts = posts,
					onAllPeople = { onTabChange(SearchTab.PEOPLE) },
					onRetryPeople = onRetryPeople,
					onRetryPosts = onRetryPosts,
					onLoadMorePosts = onLoadMorePosts,
					onUserClick = onUserClick,
					onLikeClick = onLikeClick,
					onPostClick = onPostClick,
					onCommentClick = onCommentClick,
					onMediaClick = onMediaClick,
					onAuthorBlocked = onAuthorBlocked
				)

				SearchTab.PEOPLE -> SearchResults(
					uiState = people,
					onRetry = onRetryPeople,
					onLoadMore = onLoadMorePeople,
					onUserClick = onUserClick
				)

				SearchTab.POSTS -> PostResults(
					query = people.query.trim(),
					posts = posts,
					onRetry = onRetryPosts,
					onLoadMore = onLoadMorePosts,
					onUserClick = onUserClick,
					onLikeClick = onLikeClick,
					onPostClick = onPostClick,
					onCommentClick = onCommentClick,
					onMediaClick = onMediaClick,
					onAuthorBlocked = onAuthorBlocked
				)
			}
		}
	}
}

@Composable
private fun SearchTabs(selected: SearchTab, onSelect: (SearchTab) -> Unit) {
	Row(modifier = Modifier.fillMaxWidth()) {
		SearchTab.entries.forEach { tab ->
			val isSelected = tab == selected
			Column(
				modifier = Modifier
					.weight(1f)
					.semantics {
						role = Role.Tab
						this.selected = isSelected
					}
					.clickable { onSelect(tab) }
					.padding(top = 12.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = stringResource(tab.labelRes),
					style = MaterialTheme.typography.titleSmall,
					fontWeight = FontWeight.SemiBold,
					color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
				)
				Spacer(modifier = Modifier.height(10.dp))
				Box(
					modifier = Modifier
						.fillMaxWidth(0.5f)
						.height(2.dp)
						.background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
				)
			}
		}
	}
}

@Composable
private fun TopResults(
	people: SearchUserUiState,
	posts: FeedUiState,
	onAllPeople: () -> Unit,
	onRetryPeople: () -> Unit,
	onRetryPosts: () -> Unit,
	onLoadMorePosts: () -> Unit,
	onUserClick: (userId: String, username: String) -> Unit,
	onLikeClick: (String) -> Unit,
	onPostClick: (FeedContentItem) -> Unit,
	onCommentClick: (FeedContentItem) -> Unit,
	onMediaClick: (FeedContentItem, page: Int) -> Unit,
	onAuthorBlocked: () -> Unit
) {
	val query = people.query.trim()
	val peopleDone = people.hasLoadedInitialPage && people.items.isEmpty()
	val postsDone = posts.hasLoadedInitialPage && posts.items.isEmpty() && !posts.isRefreshing
	if (peopleDone && postsDone) {
		SearchMessage(stringResource(R.string.no_results_for, query))
		return
	}

	val listState = rememberLazyListState()
	LoadMoreNearEnd(listState, onLoadMorePosts)
	LazyColumn(
		state = listState,
		modifier = Modifier
			.fillMaxSize()
			.dismissKeyboardOnScroll(),
		contentPadding = PaddingValues(bottom = 28.dp)
	) {
		when {
			people.errorMessage != null && people.items.isEmpty() -> item(contentType = "people_error") {
				RetryRow(message = people.errorMessage, onRetry = onRetryPeople)
			}

			!people.hasLoadedInitialPage -> item(contentType = "loading") { LoadingRow() }

			people.items.isNotEmpty() -> {
				item(contentType = "section") { SectionTitle(R.string.search_tab_people) }
				items(people.items.take(TOP_PEOPLE), key = { "user-${it.id}" }, contentType = { "user_item" }) { user ->
					Box(modifier = Modifier.padding(horizontal = 16.dp)) {
						SearchUserItem(user = user, onClick = { onUserClick(user.id, user.username) })
					}
				}
				if (people.items.size > TOP_PEOPLE || people.hasMore) {
					item(contentType = "see_all") {
						Text(
							text = stringResource(R.string.search_see_all_people),
							color = MaterialTheme.colorScheme.primary,
							fontWeight = FontWeight.SemiBold,
							modifier = Modifier
								.fillMaxWidth()
								.clickable(onClick = onAllPeople)
								.padding(horizontal = 16.dp, vertical = 12.dp)
						)
					}
				}
				item(contentType = "divider") { HorizontalDivider() }
			}
		}

		if (!postsDone) {
			item(contentType = "section") { SectionTitle(R.string.search_tab_posts) }
			postItems(
				posts = posts,
				onRetry = onRetryPosts,
				onUserClick = onUserClick,
				onLikeClick = onLikeClick,
				onPostClick = onPostClick,
				onCommentClick = onCommentClick,
				onMediaClick = onMediaClick,
				onAuthorBlocked = onAuthorBlocked
			)
		}
	}
}

@Composable
private fun PostResults(
	query: String,
	posts: FeedUiState,
	onRetry: () -> Unit,
	onLoadMore: () -> Unit,
	onUserClick: (userId: String, username: String) -> Unit,
	onLikeClick: (String) -> Unit,
	onPostClick: (FeedContentItem) -> Unit,
	onCommentClick: (FeedContentItem) -> Unit,
	onMediaClick: (FeedContentItem, page: Int) -> Unit,
	onAuthorBlocked: () -> Unit
) {
	if (posts.hasLoadedInitialPage && posts.items.isEmpty() && !posts.isRefreshing) {
		SearchMessage(stringResource(R.string.search_no_posts, query))
		return
	}
	val listState = rememberLazyListState()
	LoadMoreNearEnd(listState, onLoadMore)
	LazyColumn(
		state = listState,
		modifier = Modifier
			.fillMaxSize()
			.dismissKeyboardOnScroll(),
		contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
	) {
		postItems(
			posts = posts,
			onRetry = onRetry,
			onUserClick = onUserClick,
			onLikeClick = onLikeClick,
			onPostClick = onPostClick,
			onCommentClick = onCommentClick,
			onMediaClick = onMediaClick,
			onAuthorBlocked = onAuthorBlocked
		)
	}
}

/** The matching posts as feed cards, without inline video playback: a video shows its poster and opens full screen. */
private fun LazyListScope.postItems(
	posts: FeedUiState,
	onRetry: () -> Unit,
	onUserClick: (userId: String, username: String) -> Unit,
	onLikeClick: (String) -> Unit,
	onPostClick: (FeedContentItem) -> Unit,
	onCommentClick: (FeedContentItem) -> Unit,
	onMediaClick: (FeedContentItem, page: Int) -> Unit,
	onAuthorBlocked: () -> Unit
) {
	items(posts.items, key = { "post-${it.id}" }, contentType = { "feed_item" }) { post ->
		Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
			FeedItemCard(
				item = post,
				onLikeClick = onLikeClick,
				onCommentClick = onCommentClick,
				onClick = { if (!post.isTakenDown) onPostClick(post) },
				onMediaClick = { page -> onMediaClick(post, page) },
				onAuthorBlocked = onAuthorBlocked,
				onAuthorClick = onUserClick
			)
		}
	}
	when {
		posts.errorMessage != null -> item(contentType = "posts_error") {
			RetryRow(message = posts.errorMessage, onRetry = onRetry)
		}

		!posts.hasLoadedInitialPage || posts.isRefreshing || posts.isLoadingMore -> item(contentType = "loading") {
			LoadingRow()
		}
	}
}

@Composable
private fun LoadMoreNearEnd(listState: LazyListState, onLoadMore: () -> Unit) {
	val currentOnLoadMore by rememberUpdatedState(onLoadMore)
	LaunchedEffect(listState) {
		snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
			.filterNotNull()
			.collect { last ->
				if (last >= listState.layoutInfo.totalItemsCount - LOAD_AHEAD) currentOnLoadMore()
			}
	}
}

// the keyboard goes away as soon as the results scroll
@Composable
private fun Modifier.dismissKeyboardOnScroll(): Modifier {
	val focusManager = LocalFocusManager.current
	val connection = remember(focusManager) {
		object : NestedScrollConnection {
			override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
				if (available.y != 0f) focusManager.clearFocus()
				return Offset.Zero
			}
		}
	}
	return nestedScroll(connection)
}

@Composable
private fun SectionTitle(@StringRes textRes: Int) {
	Text(
		text = stringResource(textRes),
		style = MaterialTheme.typography.labelLarge,
		color = MaterialTheme.colorScheme.onSurfaceVariant,
		modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
	)
}

@Composable
private fun LoadingRow() {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.padding(24.dp),
		contentAlignment = Alignment.Center
	) {
		CircularProgressIndicator(modifier = Modifier.size(28.dp))
	}
}

@Composable
private fun RetryRow(message: String, onRetry: () -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		Text(text = message, textAlign = TextAlign.Center)
		Button(onClick = onRetry) { Text(stringResource(R.string.try_again)) }
	}
}

@Composable
private fun SearchMessage(text: String) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Icon(
			imageVector = Icons.Default.Search,
			contentDescription = null,
			modifier = Modifier.size(48.dp),
			tint = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Spacer(modifier = Modifier.height(12.dp))
		Text(text = text, textAlign = TextAlign.Center)
	}
}

@Composable
private fun SearchHint() {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = stringResource(R.string.search_hint_title),
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.SemiBold
		)
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = stringResource(R.string.search_hint_body),
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
		)
	}
}
