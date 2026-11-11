package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.CommentItem
import kotlinx.coroutines.flow.collectLatest

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 08/03/26
 * Description: Software Engineer
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentSectionSheet(
	postId: String,
	viewModel: CommentViewModel = hiltViewModel(),
	onDismiss: () -> Unit
) {
	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

	ModalBottomSheet(
		onDismissRequest = onDismiss,
		sheetState = sheetState,
		dragHandle = { BottomSheetDefaults.DragHandle() },
		containerColor = MaterialTheme.colorScheme.surface,
		tonalElevation = 8.dp
	) {
		CommentSectionContent(
			postId = postId,
			viewModel = viewModel,
			modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)
		)
	}
}

@Composable
fun CommentSectionContent(
	postId: String,
	viewModel: CommentViewModel = hiltViewModel(),
	modifier: Modifier = Modifier
) {
	val comments by viewModel.comments.collectAsState()
	val replies by viewModel.replies.collectAsState()
	val replyTarget by viewModel.replyTarget.collectAsState()
	val isLoading by viewModel.isLoading.collectAsState()
	val isSubmitting by viewModel.isSubmitting.collectAsState()
	val hasMore by viewModel.hasMore.collectAsState()
	val errorMessage by viewModel.errorMessage.collectAsState()
	val listState = rememberLazyListState()
	val snackbarHostState = remember { SnackbarHostState() }
	val inputFocusRequester = remember { FocusRequester() }
	var commentText by rememberSaveable { mutableStateOf("") }

	val rows = remember(comments, replies) { buildCommentRows(comments, replies) }
	val latestRows by rememberUpdatedState(rows)

	LaunchedEffect(postId) {
		viewModel.loadComments(postId, isRefresh = true)
	}
	LaunchedEffect(listState, comments.size, isLoading, hasMore, errorMessage) {
		snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
			.collectLatest { last ->
				val total = listState.layoutInfo.totalItemsCount
				if (comments.isNotEmpty() && last != null && last >= total - 2 &&
					hasMore && !isLoading && errorMessage == null
				) viewModel.loadMore()
			}
	}
	// tapping "Balas" moves the cursor to the input so the keyboard is ready
	LaunchedEffect(replyTarget?.id) {
		if (replyTarget != null) inputFocusRequester.requestFocus()
	}
	LaunchedEffect(viewModel) {
		viewModel.events.collect { event ->
			when (event) {
				is CommentEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
				CommentEvent.Sent -> commentText = ""
				CommentEvent.ScrollToBottom -> {
					val last = latestRows.lastIndex
					if (last >= 0) listState.animateScrollToItem(last)
				}
				is CommentEvent.ScrollToLastReply -> {
					val index = latestRows.lastReplyIndex(event.rootId)
					if (index >= 0) listState.animateScrollToItem(index)
				}
			}
		}
	}

	Column(modifier = modifier) {
		// Header
		Text(
			text = "Komentar",
			style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
			modifier = Modifier
				.fillMaxWidth()
				.padding(bottom = 8.dp),
			textAlign = TextAlign.Center
		)

		HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

		// Comments List
		Box(modifier = Modifier.weight(1f, fill = true).heightIn(min = 120.dp)) {
			if (errorMessage != null && comments.isEmpty() && !isLoading) {
				Column(
					modifier = Modifier.fillMaxSize(),
					verticalArrangement = Arrangement.Center,
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Text(errorMessage.orEmpty())
					Button(onClick = viewModel::loadMore) { Text("Coba lagi") }
				}
			} else if (comments.isEmpty() && !isLoading) {
				EmptyCommentsView()
			} else {
				LazyColumn(
					state = listState,
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(16.dp),
				) {
					itemsIndexed(items = rows, key = { _, row -> row.key }) { index, row ->
						CommentListRowContent(
							row = row,
							threadLine = rows.threadLineAt(index),
							replyTargetId = replyTarget?.id,
							modifier = Modifier.animateItem(),
							onReplyClick = viewModel::startReply,
							onToggleReplies = viewModel::toggleReplies,
							onLoadMoreReplies = viewModel::loadMoreReplies
						)
					}

					if (isLoading) {
						item(key = "loading") {
							CircularProgressIndicator(
								modifier = Modifier
									.fillMaxWidth()
									.padding(16.dp)
									.wrapContentWidth()
							)
						}
					}
					if (errorMessage != null && comments.isNotEmpty()) {
						item(key = "error") {
							Button(onClick = viewModel::loadMore) { Text("Coba lagi") }
						}
					}
				}
			}
		}

		SnackbarHost(hostState = snackbarHostState)

		// Sticky Bottom Input Area
		CommentInputArea(
			text = commentText,
			onValueChange = { commentText = it },
			onSend = { viewModel.addComment(postId, commentText) },
			replyingTo = replyTarget?.username,
			onCancelReply = viewModel::cancelReply,
			isSubmitting = isSubmitting,
			focusRequester = inputFocusRequester
		)
	}
}

@Composable
private fun CommentListRowContent(
	row: CommentListRow,
	threadLine: ThreadLine,
	replyTargetId: String?,
	onReplyClick: (CommentItem) -> Unit,
	onToggleReplies: (String) -> Unit,
	onLoadMoreReplies: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	when (row) {
		is CommentListRow.Item -> CommentRow(
			comment = row.comment,
			onReplyClick = onReplyClick,
			modifier = modifier,
			isReplyTarget = row.comment.id == replyTargetId,
			threadLine = threadLine
		)

		is CommentListRow.RepliesToggle -> ThreadControl(threadLine, modifier) {
			RepliesActionText(
				text = if (row.expanded) "Sembunyikan balasan" else "Lihat ${row.replyCount} balasan",
				onClick = { onToggleReplies(row.rootId) }
			)
		}

		is CommentListRow.RepliesMore -> ThreadControl(threadLine, modifier) {
			RepliesActionText(
				text = if (row.remaining != null) "Lihat ${row.remaining} balasan lainnya" else "Lihat balasan lainnya",
				onClick = { onLoadMoreReplies(row.rootId) }
			)
		}

		is CommentListRow.RepliesLoading -> ThreadControl(threadLine, modifier) {
			CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
		}

		is CommentListRow.RepliesError -> ThreadControl(threadLine, modifier) {
			RepliesActionText(
				text = "Gagal memuat balasan. Coba lagi",
				onClick = { onLoadMoreReplies(row.rootId) },
				isError = true
			)
		}
	}
}

/** A reply control sits on the thread line like a reply does, with the elbow at the middle of its text. */
@Composable
private fun ThreadControl(
	threadLine: ThreadLine,
	modifier: Modifier,
	content: @Composable () -> Unit
) {
	val endsThread = threadLine == ThreadLine.LastBranch
	Box(
		modifier = modifier
			.fillMaxWidth()
			.threadLine(threadLine, threadLineColor(), elbowY = 14.dp)
			.padding(bottom = if (endsThread) 14.dp else 0.dp)
			.heightIn(min = 28.dp)
			.padding(start = ThreadIndent),
		contentAlignment = Alignment.CenterStart
	) {
		content()
	}
}

@Composable
private fun RepliesActionText(text: String, onClick: () -> Unit, isError: Boolean = false) {
	Text(
		text = text,
		style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
		color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
		modifier = Modifier
			.clip(RoundedCornerShape(8.dp))
			.clickable(onClick = onClick)
			.padding(vertical = 4.dp, horizontal = 2.dp)
	)
}


@Composable
fun EmptyCommentsView() {
	Column(
		modifier = Modifier.fillMaxSize(),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = "Belum ada komentar",
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Text(
			text = "Jadilah yang pertama memulai obrolan!",
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}
