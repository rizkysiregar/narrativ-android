package com.rizkysiregar.narrativ.ui.feature.home.thread

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.domain.moderation.ReportTarget
import com.rizkysiregar.narrativ.ui.components.CommentPlaneFlight
import com.rizkysiregar.narrativ.ui.components.PostAuthorAvatar
import com.rizkysiregar.narrativ.ui.feature.home.feed.CommentEvent
import com.rizkysiregar.narrativ.ui.feature.home.feed.CommentInputArea
import com.rizkysiregar.narrativ.ui.feature.home.feed.CommentListRowContent
import com.rizkysiregar.narrativ.ui.feature.home.feed.CommentViewModel
import com.rizkysiregar.narrativ.ui.feature.home.feed.DeleteCommentDialog
import com.rizkysiregar.narrativ.ui.feature.home.feed.EmptyCommentsView
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostDetailHandoff
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostDetailMessage
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostDetailUiState
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostMediaPager
import com.rizkysiregar.narrativ.ui.feature.home.feed.authorChapters
import com.rizkysiregar.narrativ.ui.feature.home.feed.buildCommentRows
import com.rizkysiregar.narrativ.ui.feature.home.feed.lastReplyIndex
import com.rizkysiregar.narrativ.ui.feature.home.feed.threadLineAt
import com.rizkysiregar.narrativ.ui.feature.moderation.ContentModerationMenu
import com.rizkysiregar.narrativ.ui.feature.moderation.ModerationTarget
import com.rizkysiregar.narrativ.ui.theme.Like
import com.rizkysiregar.narrativ.util.formatPostDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// the list items above the comments: the post, then the comments heading
private const val HEADER_ITEMS = 2
private const val COMMENTS_HEADING_INDEX = 1
private const val HIGHLIGHT_MILLIS = 2500L
private val MediaShape = RoundedCornerShape(16.dp)

/**
 * A post's thread: the post in full with its comments under it, and the comment input pinned to the bottom.
 *
 * [openComments] lands on the comments with the input focused (the comment button of a card); [focusCommentId]
 * scrolls to that comment and marks it for a moment (a push or a notification about it). [onMediaClick] opens the
 * media full screen and gets the thread's own like handler, so a like there shows here too.
 */
@Composable
fun PostThreadDestination(
	postId: String,
	openComments: Boolean,
	focusCommentId: String?,
	handoff: PostDetailHandoff,
	onBackClick: () -> Unit,
	onUserClick: (userId: String, username: String) -> Unit,
	onMediaClick: (post: FeedContentItem, onLike: (String) -> Unit, page: Int) -> Unit,
	viewModel: PostThreadViewModel = hiltViewModel()
) {
	// read the hand-over in the first composition, so a post opened from a list shows without a spinner
	remember(postId) { viewModel.start(postId, handoff.postFor(postId), handoff.likeHandlerFor(postId)) }
	val state by viewModel.state.collectAsStateWithLifecycle()

	when (val current = state) {
		is PostDetailUiState.Loaded -> PostThreadScreen(
			post = current.post,
			openComments = openComments,
			focusCommentId = focusCommentId,
			onBackClick = onBackClick,
			onLikeClick = viewModel::toggleLike,
			onUserClick = onUserClick,
			onMediaClick = { page ->
				viewModel.currentPost()?.let { post -> onMediaClick(post, { viewModel.toggleLike() }, page) }
			},
			onAuthorBlocked = onBackClick
		)

		PostDetailUiState.Loading -> PostDetailMessage(onBackClick = onBackClick) { CircularProgressIndicator() }

		PostDetailUiState.Unavailable -> PostDetailMessage(onBackClick = onBackClick) {
			Text(stringResource(R.string.post_not_found), color = MaterialTheme.colorScheme.onBackground)
		}

		is PostDetailUiState.Error -> PostDetailMessage(onBackClick = onBackClick) {
			Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
				Text(current.message, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
				TextButton(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) }
			}
		}
	}
}

@Composable
private fun PostThreadScreen(
	post: FeedContentItem,
	openComments: Boolean,
	focusCommentId: String?,
	onBackClick: () -> Unit,
	onLikeClick: () -> Unit,
	onUserClick: (userId: String, username: String) -> Unit,
	onMediaClick: (page: Int) -> Unit,
	onAuthorBlocked: () -> Unit,
	commentViewModel: CommentViewModel = hiltViewModel()
) {
	val comments by commentViewModel.comments.collectAsState()
	val replies by commentViewModel.replies.collectAsState()
	val replyTarget by commentViewModel.replyTarget.collectAsState()
	val isLoading by commentViewModel.isLoading.collectAsState()
	val isSubmitting by commentViewModel.isSubmitting.collectAsState()
	val hasMore by commentViewModel.hasMore.collectAsState()
	val errorMessage by commentViewModel.errorMessage.collectAsState()

	val listState = rememberLazyListState()
	val scope = rememberCoroutineScope()
	val snackbarHostState = remember { SnackbarHostState() }
	val inputFocusRequester = remember { FocusRequester() }
	val focusManager = LocalFocusManager.current
	val keyboardController = LocalSoftwareKeyboardController.current
	val density = LocalDensity.current

	var commentText by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
	var pendingDelete by remember { mutableStateOf<CommentItem?>(null) }
	// where the send button is, in root coordinates, and where a plane is flying from (null while none is)
	var sendOrigin by remember { mutableStateOf(Offset.Zero) }
	var planeOrigin by remember { mutableStateOf<Offset?>(null) }
	// openComments and focusCommentId are acted on once, not again when the user comes back to the thread
	var landed by rememberSaveable { mutableStateOf(false) }
	var highlightedId by remember { mutableStateOf<String?>(null) }

	val rows = remember(comments, replies) { buildCommentRows(comments, replies) }
	val chapters = remember(comments, post.userId) { authorChapters(comments, post.userId) }
	val latestRows by rememberUpdatedState(rows)
	val firstPageDone = comments.isNotEmpty() || (!isLoading && (errorMessage != null || !hasMore))

	fun scrollToRow(index: Int) {
		if (index >= 0) scope.launch { listState.animateScrollToItem(index + HEADER_ITEMS) }
	}

	LaunchedEffect(post.id) { commentViewModel.start(post.id) }

	LaunchedEffect(listState, comments.size, isLoading, hasMore, errorMessage) {
		snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
			.collectLatest { last ->
				val total = listState.layoutInfo.totalItemsCount
				if (comments.isNotEmpty() && last != null && last >= total - 2 &&
					hasMore && !isLoading && errorMessage == null
				) commentViewModel.loadMore()
			}
	}

	LaunchedEffect(firstPageDone) {
		if (landed || !firstPageDone || (!openComments && focusCommentId == null)) return@LaunchedEffect
		landed = true
		withFrameNanos { } // let the comment rows be composed first
		// a reply that is not shown (its thread collapsed) or a comment on a later page cannot be found: then the
		// thread lands on the comments heading
		val target = focusCommentId?.let { id -> latestRows.indexOfFirst { it.key == id } }?.takeIf { it >= 0 }
		if (target != null) {
			listState.animateScrollToItem(target + HEADER_ITEMS)
			highlightedId = focusCommentId
		} else {
			listState.animateScrollToItem(COMMENTS_HEADING_INDEX)
			if (focusCommentId == null) inputFocusRequester.requestFocus()
		}
	}
	LaunchedEffect(highlightedId) {
		if (highlightedId != null) {
			delay(HIGHLIGHT_MILLIS)
			highlightedId = null
		}
	}

	// tapping "Balas" moves the cursor to the input so the keyboard is ready
	LaunchedEffect(replyTarget?.id) {
		if (replyTarget != null) inputFocusRequester.requestFocus()
	}
	LaunchedEffect(commentViewModel) {
		commentViewModel.events.collect { event ->
			when (event) {
				is CommentEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
				CommentEvent.Sent -> {
					commentText = TextFieldValue("")
					focusManager.clearFocus()
					keyboardController?.hide()
					planeOrigin = sendOrigin
				}
				is CommentEvent.SendFailed -> commentText = TextFieldValue(event.text, TextRange(event.text.length))
				CommentEvent.ScrollToBottom -> {
					withFrameNanos { } // let the new row be composed first
					val last = latestRows.lastIndex
					if (last >= 0) listState.animateScrollToItem(last + HEADER_ITEMS)
				}
				is CommentEvent.ScrollToLastReply -> {
					withFrameNanos { }
					val index = latestRows.lastReplyIndex(event.rootId)
					if (index >= 0) listState.animateScrollToItem(index + HEADER_ITEMS)
				}
			}
		}
	}

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			ThreadTopBar(onBackClick = onBackClick)
			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

			Box(modifier = Modifier.weight(1f)) {
				LazyColumn(
					state = listState,
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(bottom = 16.dp)
				) {
					item(key = "post") {
						ThreadPost(
							post = post,
							continuationCount = chapters.size,
							onLikeClick = onLikeClick,
							onCommentClick = { inputFocusRequester.requestFocus() },
							onUserClick = onUserClick,
							onMediaClick = onMediaClick,
							onAuthorBlocked = onAuthorBlocked,
							onContinuationClick = {
								val first = chapters.minByOrNull { it.value }?.key
								scrollToRow(rows.indexOfFirst { it.key == first })
							}
						)
					}
					item(key = "comments-heading") {
						CommentsHeading(count = post.commentCount)
					}

					itemsIndexed(items = rows, key = { _, row -> row.key }) { index, row ->
						CommentListRowContent(
							row = row,
							threadLine = rows.threadLineAt(index),
							replyTargetId = replyTarget?.id,
							modifier = Modifier
								.animateItem()
								.padding(horizontal = 16.dp),
							onReplyClick = commentViewModel::startReply,
							onToggleReplies = commentViewModel::toggleReplies,
							onLoadMoreReplies = commentViewModel::loadMoreReplies,
							onDeleteRequest = { pendingDelete = it },
							onAuthorBlocked = { commentViewModel.loadComments(post.id, isRefresh = true) },
							postAuthorId = post.userId,
							chapters = chapters,
							highlightedId = highlightedId
						)
					}

					when {
						isLoading -> item(key = "loading") {
							CircularProgressIndicator(
								modifier = Modifier
									.fillMaxWidth()
									.padding(16.dp)
									.wrapContentWidth()
							)
						}

						errorMessage != null -> item(key = "error") {
							Column(
								modifier = Modifier
									.fillMaxWidth()
									.padding(16.dp),
								horizontalAlignment = Alignment.CenterHorizontally
							) {
								if (comments.isEmpty()) Text(errorMessage.orEmpty(), textAlign = TextAlign.Center)
								Button(onClick = commentViewModel::loadMore) { Text(stringResource(R.string.try_again)) }
							}
						}

						comments.isEmpty() && !hasMore -> item(key = "empty") {
							EmptyCommentsView(onWriteClick = { inputFocusRequester.requestFocus() })
						}
					}
				}

				SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
			}

			CommentInputArea(
				text = commentText,
				onValueChange = { commentText = it },
				onSend = { commentViewModel.addComment(post.id, commentText.text) },
				replyingTo = replyTarget?.username,
				onCancelReply = commentViewModel::cancelReply,
				isSubmitting = isSubmitting,
				focusRequester = inputFocusRequester,
				modifier = Modifier.onGloballyPositioned { coordinates ->
					// the send button sits at the end of the input
					val bounds = coordinates.boundsInRoot()
					sendOrigin = Offset(bounds.right - with(density) { 32.dp.toPx() }, bounds.top + with(density) { 28.dp.toPx() })
				}
			)
		}

		planeOrigin?.let { origin ->
			CommentPlaneFlight(origin = origin, onFinished = { planeOrigin = null })
		}
	}

	pendingDelete?.let { target ->
		DeleteCommentDialog(
			comment = target,
			onConfirm = {
				commentViewModel.deleteComment(target)
				pendingDelete = null
			},
			onDismiss = { pendingDelete = null }
		)
	}
}

@Composable
private fun ThreadTopBar(onBackClick: () -> Unit) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 4.dp, vertical = 4.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		IconButton(onClick = onBackClick) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.ArrowBack,
				contentDescription = stringResource(R.string.back),
				tint = MaterialTheme.colorScheme.onBackground
			)
		}
		Text(
			text = stringResource(R.string.thread_title),
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.SemiBold,
			color = MaterialTheme.colorScheme.onBackground
		)
	}
}

/** The post itself: author, the whole caption, the media, and its like and comment counts. */
@Composable
private fun ThreadPost(
	post: FeedContentItem,
	continuationCount: Int,
	onLikeClick: () -> Unit,
	onCommentClick: () -> Unit,
	onUserClick: (userId: String, username: String) -> Unit,
	onMediaClick: (page: Int) -> Unit,
	onAuthorBlocked: () -> Unit,
	onContinuationClick: () -> Unit
) {
	Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)) {
		ThreadPostHeader(post = post, onUserClick = onUserClick, onAuthorBlocked = onAuthorBlocked)

		if (!post.caption.isNullOrBlank()) {
			Spacer(Modifier.height(12.dp))
			Text(
				text = post.caption,
				style = if (post.hasMedia) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
				color = MaterialTheme.colorScheme.onSurface
			)
		}

		if (continuationCount > 0) {
			Spacer(Modifier.height(10.dp))
			ContinuationHint(count = continuationCount, onClick = onContinuationClick)
		}

		if (post.hasMedia) {
			Spacer(Modifier.height(12.dp))
			// no inline playback: a video shows its poster and plays full screen
			PostMediaPager(
				media = post.mediaItems,
				isActive = false,
				player = null,
				muted = true,
				onMediaClick = onMediaClick,
				aspectRatio = 1f,
				modifier = Modifier.clip(MediaShape)
			)
		}

		if (post.createdAt.isNotBlank()) {
			Spacer(Modifier.height(12.dp))
			Text(
				text = formatPostDate(post.createdAt),
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}

		ThreadActions(post = post, onLikeClick = onLikeClick, onCommentClick = onCommentClick)
	}
}

@Composable
private fun ThreadPostHeader(
	post: FeedContentItem,
	onUserClick: (userId: String, username: String) -> Unit,
	onAuthorBlocked: () -> Unit
) {
	// an older server may not send the author's id, then there is nowhere to go
	val authorClick = post.userId?.let { id ->
		Modifier.clickable(
			interactionSource = remember { MutableInteractionSource() },
			indication = null
		) { onUserClick(id, post.username) }
	} ?: Modifier

	Row(verticalAlignment = Alignment.CenterVertically) {
		PostAuthorAvatar(imageUrl = post.profileImageUrl, name = post.username, size = 42.dp, modifier = authorClick)
		Spacer(Modifier.width(12.dp))
		Text(
			text = post.username,
			style = MaterialTheme.typography.titleSmall,
			fontWeight = FontWeight.SemiBold,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier
				.weight(1f)
				.then(authorClick)
		)
		ContentModerationMenu(
			target = ModerationTarget(ReportTarget.POST, post.id, post.userId, post.username),
			onBlocked = onAuthorBlocked
		)
	}
}

/** "The story goes on": the author added parts in the comments; a tap scrolls to the first one. */
@Composable
private fun ContinuationHint(count: Int, onClick: () -> Unit) {
	Row(
		modifier = Modifier
			.clip(RoundedCornerShape(50))
			.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
			.clickable(onClick = onClick)
			.padding(horizontal = 12.dp, vertical = 6.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = stringResource(R.string.thread_continues, count),
			style = MaterialTheme.typography.labelLarge,
			fontWeight = FontWeight.SemiBold,
			color = MaterialTheme.colorScheme.primary
		)
	}
}

@Composable
private fun ThreadActions(post: FeedContentItem, onLikeClick: () -> Unit, onCommentClick: () -> Unit) {
	val likeScale by animateFloatAsState(
		targetValue = if (post.liked) 1.12f else 1f,
		animationSpec = tween(durationMillis = 180),
		label = "thread-like-scale"
	)
	val likeColor by animateColorAsState(
		targetValue = if (post.liked) Like else MaterialTheme.colorScheme.onSurfaceVariant,
		animationSpec = tween(durationMillis = 180),
		label = "thread-like-color"
	)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 6.dp),
		horizontalArrangement = Arrangement.spacedBy(4.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		ThreadActionButton(onClick = onLikeClick) {
			Icon(
				imageVector = if (post.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
				contentDescription = if (post.liked) stringResource(R.string.cd_unlike) else stringResource(R.string.cd_like),
				tint = likeColor,
				modifier = Modifier
					.size(22.dp)
					.scale(likeScale)
			)
			Text(
				text = post.likeCount.toString(),
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		ThreadActionButton(onClick = onCommentClick) {
			Icon(
				painter = painterResource(R.drawable.comment_regular_full),
				contentDescription = stringResource(R.string.write_comment),
				tint = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.size(20.dp)
			)
			Text(
				text = post.commentCount.toString(),
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
private fun ThreadActionButton(onClick: () -> Unit, content: @Composable () -> Unit) {
	Row(
		modifier = Modifier
			.clip(RoundedCornerShape(50))
			.clickable(onClick = onClick)
			.padding(horizontal = 10.dp, vertical = 8.dp),
		horizontalArrangement = Arrangement.spacedBy(6.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		content()
	}
}

@Composable
private fun CommentsHeading(count: Int) {
	Column {
		HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
		Text(
			text = if (count > 0) "${stringResource(R.string.comments)} · $count" else stringResource(R.string.comments),
			style = MaterialTheme.typography.titleSmall,
			fontWeight = FontWeight.Bold,
			color = MaterialTheme.colorScheme.onBackground,
			modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
		)
	}
}
