package com.rizkysiregar.narrativ.ui.feature.home.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.ui.components.PostTileContent
import com.rizkysiregar.narrativ.ui.components.TakenDownPlaceholder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.rizkysiregar.narrativ.R

enum class PostViewMode(val icon: ImageVector, @StringRes val labelRes: Int) {
	GRID(Icons.Outlined.GridView, R.string.view_grid),
	LIST(Icons.Outlined.ViewAgenda, R.string.view_list)
}

private const val GRID_LOAD_AHEAD = 6
private const val LIST_LOAD_AHEAD = 3

/**
 * A grid/list switch above two swipeable pages: the grid shows [gridFeed] (posts with media only),
 * the list [listFeed] (every post as a card). With a [header] (a profile), the header scrolls away
 * and the switch stays pinned on top. Page 0 is the grid.
 */
@Composable
fun PostViewPager(
	pagerState: PagerState,
	gridFeed: FeedUiState,
	listFeed: FeedUiState,
	// a grid tile or a card: opens the post's thread
	onPostClick: (FeedContentItem) -> Unit,
	onLikeClick: (String) -> Unit,
	onCommentClick: (FeedContentItem) -> Unit,
	onMediaClick: (FeedContentItem, page: Int) -> Unit,
	onLoadMoreGrid: () -> Unit,
	onLoadMoreList: () -> Unit,
	onRetryGrid: () -> Unit,
	onRetryList: () -> Unit,
	modifier: Modifier = Modifier,
	header: (@Composable () -> Unit)? = null,
	onAuthorBlocked: (() -> Unit)? = null,
	onAuthorClick: ((userId: String, username: String) -> Unit)? = null
) {
	// hoisted so each page keeps its scroll position while swiped out of composition
	val gridState = rememberLazyGridState()
	val listState = rememberLazyListState()

	val pages: @Composable () -> Unit = {
		HorizontalPager(
			state = pagerState,
			modifier = Modifier.fillMaxSize(),
			key = { PostViewMode.entries[it] }
		) { page ->
			when (PostViewMode.entries[page]) {
				PostViewMode.GRID -> PostGridPage(
					feed = gridFeed,
					gridState = gridState,
					onPostClick = onPostClick,
					onLoadMore = onLoadMoreGrid,
					onRetry = onRetryGrid
				)

				PostViewMode.LIST -> PostListPage(
					feed = listFeed,
					listState = listState,
					onLikeClick = onLikeClick,
					onPostClick = onPostClick,
					onCommentClick = onCommentClick,
					onMediaClick = onMediaClick,
					onLoadMore = onLoadMoreList,
					onRetry = onRetryList,
					onAuthorBlocked = onAuthorBlocked,
					onAuthorClick = onAuthorClick
				)
			}
		}
	}

	if (header != null) {
		CollapsingHeaderLayout(
			header = header,
			pinned = { PostViewModeTabs(pagerState = pagerState) },
			content = pages,
			modifier = modifier
		)
	} else {
		Column(modifier = modifier) {
			PostViewModeTabs(pagerState = pagerState)
			Box(modifier = Modifier.weight(1f)) { pages() }
		}
	}
}

/** Grid and list icons; the indicator under them follows the pager while it is swiped. */
@Composable
fun PostViewModeTabs(pagerState: PagerState, modifier: Modifier = Modifier) {
	val scope = rememberCoroutineScope()
	val modes = PostViewMode.entries

	Column(
		modifier = modifier
			.fillMaxWidth()
			.background(MaterialTheme.colorScheme.background)
	) {
		Row(modifier = Modifier.fillMaxWidth()) {
			modes.forEachIndexed { index, mode ->
				val selected = pagerState.currentPage == index
				val tint by animateColorAsState(
					targetValue = if (selected) MaterialTheme.colorScheme.onSurface
					else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
					label = "view-mode-tint"
				)
				Box(
					modifier = Modifier
						.weight(1f)
						.height(46.dp)
						.clickable { scope.launch { pagerState.animateScrollToPage(index) } },
					contentAlignment = Alignment.Center
				) {
					Icon(
						imageVector = mode.icon,
						contentDescription = stringResource(mode.labelRes),
						tint = tint,
						modifier = Modifier.size(22.dp)
					)
				}
			}
		}
		BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(2.dp)) {
			val tabWidth = maxWidth / modes.size
			val indicatorWidth = 28.dp
			Box(
				modifier = Modifier
					.offset {
						val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
						val x = tabWidth.toPx() * position + (tabWidth - indicatorWidth).toPx() / 2
						IntOffset(x.roundToInt(), 0)
					}
					.width(indicatorWidth)
					.fillMaxSize()
					.background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(50))
			)
		}
		HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
	}
}

@Composable
private fun PostGridPage(
	feed: FeedUiState,
	gridState: LazyGridState,
	onPostClick: (FeedContentItem) -> Unit,
	onLoadMore: () -> Unit,
	onRetry: () -> Unit
) {
	LoadMoreEffect(
		feed = feed,
		isNearEnd = {
			val info = gridState.layoutInfo
			val last = info.visibleItemsInfo.lastOrNull()?.index
			last != null && last >= info.totalItemsCount - GRID_LOAD_AHEAD
		},
		onLoadMore = onLoadMore
	)

	LazyVerticalGrid(
		state = gridState,
		columns = GridCells.Fixed(3),
		horizontalArrangement = Arrangement.spacedBy(2.dp),
		verticalArrangement = Arrangement.spacedBy(2.dp),
		modifier = Modifier.fillMaxSize()
	) {
		items(feed.items.size, key = { feed.items[it].id }) { index ->
			val post = feed.items[index]
			Surface(
				modifier = Modifier
					.fillMaxWidth()
					.aspectRatio(1f)
					.clickable(enabled = !post.isTakenDown) { onPostClick(post) }
			) {
				if (post.isTakenDown) {
					TakenDownPlaceholder(
						message = post.takedownMessage,
						modifier = Modifier.fillMaxSize(),
						compact = true
					)
				} else {
					PostTileContent(post = post)
				}
			}
		}

		item(span = { GridItemSpan(maxLineSpan) }) {
			PageStatus(feed = feed, emptyMessage = stringResource(R.string.no_media_yet), onRetry = onRetry)
		}
	}
}

@Composable
private fun PostListPage(
	feed: FeedUiState,
	listState: LazyListState,
	onLikeClick: (String) -> Unit,
	onPostClick: (FeedContentItem) -> Unit,
	onCommentClick: (FeedContentItem) -> Unit,
	onMediaClick: (FeedContentItem, page: Int) -> Unit,
	onLoadMore: () -> Unit,
	onRetry: () -> Unit,
	onAuthorBlocked: (() -> Unit)? = null,
	onAuthorClick: ((userId: String, username: String) -> Unit)? = null
) {
	LoadMoreEffect(
		feed = feed,
		isNearEnd = {
			val info = listState.layoutInfo
			val last = info.visibleItemsInfo.lastOrNull()?.index
			last != null && last >= info.totalItemsCount - LIST_LOAD_AHEAD
		},
		onLoadMore = onLoadMore
	)

	LazyColumn(
		state = listState,
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 28.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		items(feed.items, key = { it.id }) { post ->
			// no inline playback here: a video shows its poster and opens full screen
			FeedItemCard(
				item = post,
				onLikeClick = onLikeClick,
				onCommentClick = onCommentClick,
				onClick = { onPostClick(post) },
				onMediaClick = { page -> onMediaClick(post, page) },
				onAuthorBlocked = onAuthorBlocked,
				onAuthorClick = onAuthorClick
			)
		}

		item {
			PageStatus(feed = feed, emptyMessage = stringResource(R.string.no_posts_yet), onRetry = onRetry)
		}
	}
}

/** Footer of a page: first load, empty, error with retry, or loading the next page. */
@Composable
private fun PageStatus(feed: FeedUiState, emptyMessage: String, onRetry: () -> Unit) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 24.dp),
		contentAlignment = Alignment.Center
	) {
		when {
			feed.isError -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
				if (feed.items.isEmpty()) {
					Text(
						text = feed.errorMessage ?: stringResource(R.string.error_load_posts),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						textAlign = TextAlign.Center
					)
					Spacer(Modifier.height(8.dp))
				}
				Button(onClick = onRetry) { Text(stringResource(R.string.try_again)) }
			}

			feed.isLoadingMore || (feed.isRefreshing && feed.items.isEmpty()) ->
				CircularProgressIndicator(modifier = Modifier.size(28.dp))

			feed.hasLoadedInitialPage && feed.items.isEmpty() -> Text(
				text = emptyMessage,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

/** Asks for the next page once the end is near and the feed is idle with more to load. */
@Composable
private fun LoadMoreEffect(
	feed: FeedUiState,
	isNearEnd: () -> Boolean,
	onLoadMore: () -> Unit
) {
	val currentFeed by rememberUpdatedState(feed)
	val currentOnLoadMore by rememberUpdatedState(onLoadMore)
	LaunchedEffect(Unit) {
		snapshotFlow {
			val f = currentFeed
			isNearEnd() && f.hasLoadedInitialPage && f.hasMore &&
				!f.isLoadingMore && !f.isRefreshing && !f.isError
		}
			.distinctUntilChanged()
			.filter { it }
			.collect { currentOnLoadMore() }
	}
}

/**
 * [header] scrolls away first when the content is scrolled up and comes back once the content is
 * scrolled back to its top; [pinned] stays right under it and sticks to the top when it is gone.
 */
@Composable
private fun CollapsingHeaderLayout(
	header: @Composable () -> Unit,
	pinned: @Composable () -> Unit,
	content: @Composable () -> Unit,
	modifier: Modifier = Modifier
) {
	var headerHeight by remember { mutableIntStateOf(0) }
	// 0 when fully shown, -headerHeight when scrolled away
	var headerOffset by remember { mutableFloatStateOf(0f) }

	val connection = remember {
		object : NestedScrollConnection {
			override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
				if (available.y >= 0f) return Offset.Zero
				val newOffset = (headerOffset + available.y).coerceAtLeast(-headerHeight.toFloat())
				val consumed = newOffset - headerOffset
				headerOffset = newOffset
				return Offset(0f, consumed)
			}

			override fun onPostScroll(
				consumed: Offset,
				available: Offset,
				source: NestedScrollSource
			): Offset {
				if (available.y <= 0f) return Offset.Zero
				val newOffset = (headerOffset + available.y).coerceAtMost(0f)
				val consumedY = newOffset - headerOffset
				headerOffset = newOffset
				return Offset(0f, consumedY)
			}
		}
	}

	Column(modifier = modifier.nestedScroll(connection)) {
		Box(
			modifier = Modifier
				.clipToBounds()
				.layout { measurable, constraints ->
					val placeable = measurable.measure(
						constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
					)
					headerHeight = placeable.height
					val offset = headerOffset.roundToInt().coerceAtLeast(-placeable.height)
					layout(placeable.width, placeable.height + offset) {
						placeable.place(0, offset)
					}
				}
		) {
			header()
		}
		pinned()
		Box(modifier = Modifier.weight(1f)) { content() }
	}
}
