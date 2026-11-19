package com.rizkysiregar.narrativ.ui.feature.home.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.data.remote.model.response.notification.NotificationItem
import com.rizkysiregar.narrativ.ui.components.PostAuthorAvatar
import com.rizkysiregar.narrativ.util.formatTimeAgo
import kotlinx.coroutines.flow.distinctUntilChanged

private const val LOAD_AHEAD = 4

/** Where tapping an inbox entry leads: the post's thread (at the comment), or the profile of whoever followed. */
@Composable
fun NotificationsRoute(
	onBackClick: () -> Unit,
	onOpenPost: (postId: String, commentId: String?) -> Unit,
	onOpenUser: (userId: String, username: String) -> Unit,
	viewModel: NotificationsViewModel = hiltViewModel()
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	NotificationsScreen(
		state = state,
		onBackClick = onBackClick,
		onRefresh = viewModel::refresh,
		onLoadMore = viewModel::loadMore,
		onRetry = viewModel::retry,
		onItemClick = { item ->
			viewModel.onOpened(item)
			val actor = item.actors.firstOrNull()
			when {
				item.type == NotificationItem.TYPE_FOLLOWED && actor != null -> onOpenUser(actor.id, actor.username)
				item.postId != null -> onOpenPost(item.postId, item.commentId)
			}
		}
	)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NotificationsScreen(
	state: NotificationsUiState,
	onBackClick: () -> Unit,
	onRefresh: () -> Unit,
	onLoadMore: () -> Unit,
	onRetry: () -> Unit,
	onItemClick: (NotificationItem) -> Unit
) {
	val listState = rememberLazyListState()
	val pullState = rememberPullRefreshState(refreshing = state.isRefreshing, onRefresh = onRefresh)
	val sections = remember(state.items) { groupBySection(state.items) }

	LaunchedEffect(listState, state.hasMore) {
		snapshotFlow {
			val info = listState.layoutInfo
			val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
			last >= info.totalItemsCount - LOAD_AHEAD
		}.distinctUntilChanged().collect { nearEnd -> if (nearEnd && state.hasMore) onLoadMore() }
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 4.dp, vertical = 4.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			IconButton(onClick = onBackClick) {
				Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
			}
			Text(
				text = stringResource(R.string.notifications_title),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold
			)
		}
		HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

		Box(
			modifier = Modifier
				.weight(1f)
				.fillMaxWidth()
				.pullRefresh(pullState)
		) {
			when {
				!state.hasLoadedFirstPage && state.errorMessage == null -> CircularProgressIndicator(
					modifier = Modifier.align(Alignment.Center)
				)

				!state.hasLoadedFirstPage -> ErrorContent(state.errorMessage.orEmpty(), onRetry, Modifier.align(Alignment.Center))

				state.items.isEmpty() -> EmptyNotifications(Modifier.align(Alignment.Center))

				else -> LazyColumn(
					state = listState,
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(bottom = 24.dp)
				) {
					sections.forEach { (section, entries) ->
						item(key = "section-${section.name}") { SectionHeading(section) }
						items(entries, key = { it.groupKey }) { item ->
							NotificationRow(item = item, onClick = { onItemClick(item) })
						}
					}
					if (state.isLoading && !state.isRefreshing) {
						item(key = "loading") {
							CircularProgressIndicator(
								modifier = Modifier
									.fillMaxWidth()
									.padding(16.dp)
									.wrapContentWidth()
							)
						}
					} else if (state.errorMessage != null) {
						item(key = "error") { ErrorContent(state.errorMessage, onRetry, Modifier.fillMaxWidth().padding(16.dp)) }
					}
				}
			}

			PullRefreshIndicator(
				refreshing = state.isRefreshing,
				state = pullState,
				modifier = Modifier.align(Alignment.TopCenter)
			)
		}
	}
}

@Composable
private fun SectionHeading(section: NotificationSection) {
	Text(
		text = stringResource(
			when (section) {
				NotificationSection.TODAY -> R.string.notifications_today
				NotificationSection.THIS_WEEK -> R.string.notifications_this_week
				NotificationSection.EARLIER -> R.string.notifications_earlier
			}
		),
		style = MaterialTheme.typography.titleSmall,
		fontWeight = FontWeight.Bold,
		modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp)
	)
}

@Composable
private fun NotificationRow(item: NotificationItem, onClick: () -> Unit) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.background(if (item.unread) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.background)
			.clickable(onClick = onClick)
			.padding(horizontal = 16.dp, vertical = 10.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		ActorAvatars(item)
		Spacer(Modifier.width(12.dp))
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = notificationText(item),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurface,
				maxLines = 3,
				overflow = TextOverflow.Ellipsis
			)
			item.preview?.takeIf { it.isNotBlank() }?.let { preview ->
				Text(
					text = "“$preview”",
					style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.padding(top = 2.dp)
				)
			}
			Text(
				text = formatTimeAgo(item.createdAt),
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(top = 2.dp)
			)
		}
		item.postThumbnailUrl?.let { url ->
			Spacer(Modifier.width(12.dp))
			AsyncImage(
				model = url,
				contentDescription = null,
				contentScale = ContentScale.Crop,
				modifier = Modifier
					.size(48.dp)
					.clip(RoundedCornerShape(8.dp))
					.background(MaterialTheme.colorScheme.surfaceVariant)
			)
		}
		if (item.unread) {
			Spacer(Modifier.width(10.dp))
			Box(
				modifier = Modifier
					.size(8.dp)
					.clip(CircleShape)
					.background(MaterialTheme.colorScheme.primary)
			)
		}
	}
}

/** One avatar, or the newest two overlapping when more people did the same thing. */
@Composable
private fun ActorAvatars(item: NotificationItem) {
	val first = item.actors.getOrNull(0)
	val second = item.actors.getOrNull(1)
	Box(modifier = Modifier.size(44.dp)) {
		if (second == null) {
			PostAuthorAvatar(imageUrl = first?.profileImageUrl, name = first?.username.orEmpty(), size = 44.dp)
		} else {
			PostAuthorAvatar(imageUrl = second.profileImageUrl, name = second.username, size = 32.dp)
			PostAuthorAvatar(
				imageUrl = first?.profileImageUrl,
				name = first?.username.orEmpty(),
				size = 32.dp,
				modifier = Modifier
					.offset(x = 12.dp, y = 12.dp)
					.border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
			)
		}
	}
}

@Composable
private fun notificationText(item: NotificationItem): AnnotatedString {
	val first = item.actors.getOrNull(0)?.username.orEmpty()
	val second = item.actors.getOrNull(1)?.username
	val others = (item.actorCount - 2).coerceAtLeast(0)
	val text = when (item.type) {
		NotificationItem.TYPE_POST_LIKED -> when {
			second == null -> stringResource(R.string.notif_liked_one, first)
			others == 0 -> stringResource(R.string.notif_liked_two, first, second)
			else -> stringResource(R.string.notif_liked_many, first, second, others)
		}
		NotificationItem.TYPE_POST_COMMENTED -> stringResource(R.string.notif_commented, first)
		NotificationItem.TYPE_COMMENT_REPLIED -> stringResource(R.string.notif_replied, first)
		NotificationItem.TYPE_FOLLOWED -> stringResource(R.string.notif_followed, first)
		NotificationItem.TYPE_COMMENT_PINNED -> stringResource(R.string.notif_pinned, first)
		else -> first
	}
	return boldNames(text, listOfNotNull(first.takeIf { it.isNotBlank() }, second))
}

/** The people's names in bold, wherever the language put them in the sentence. */
private fun boldNames(text: String, names: List<String>): AnnotatedString = buildAnnotatedString {
	append(text)
	for (name in names) {
		val start = text.indexOf(name)
		if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + name.length)
	}
}

@Composable
private fun EmptyNotifications(modifier: Modifier = Modifier) {
	Column(
		modifier = modifier.padding(32.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(6.dp)
	) {
		Icon(
			imageVector = Icons.Outlined.Notifications,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.size(40.dp)
		)
		Spacer(Modifier.height(4.dp))
		Text(stringResource(R.string.notifications_empty_title), style = MaterialTheme.typography.titleMedium)
		Text(
			text = stringResource(R.string.notifications_empty_body),
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
		)
	}
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
	Column(modifier = modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
		Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
		Spacer(Modifier.height(8.dp))
		Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
	}
}
