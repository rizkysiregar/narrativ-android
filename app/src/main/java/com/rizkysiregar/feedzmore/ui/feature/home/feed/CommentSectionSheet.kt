package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

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
	val comments by viewModel.comments.collectAsState()
	val isLoading by viewModel.isLoading.collectAsState()
	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
	var commentText by remember { mutableStateOf("") }

	LaunchedEffect(postId) {
		viewModel.loadComments(postId, isRefresh = true)
	}

	ModalBottomSheet(
		onDismissRequest = onDismiss,
		sheetState = sheetState,
		dragHandle = { BottomSheetDefaults.DragHandle() },
		containerColor = MaterialTheme.colorScheme.surface,
		tonalElevation = 8.dp
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			// Header
			Text(
				text = "Comments",
				style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 8.dp),
				textAlign = TextAlign.Center
			)

			HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

			// Comments List
			Box(modifier = Modifier.weight(1f)) {
				if (comments.isEmpty() && !isLoading) {
					EmptyCommentsView()
				} else {
					LazyColumn(
						modifier = Modifier.fillMaxSize(),
						contentPadding = PaddingValues(16.dp),
						verticalArrangement = Arrangement.spacedBy(16.dp)
					) {
						itemsIndexed(
							items = comments
						) { index, comment ->
							CommentRow(comment)

							// Pagination
							if (index >= comments.size - 2 && !isLoading) {
								viewModel.loadMore()
							}
						}

						if (isLoading) {
							item {
								CircularProgressIndicator(
									modifier = Modifier
										.fillMaxWidth()
										.padding(16.dp)
										.wrapContentWidth()
								)
							}
						}
					}
				}
			}

			// Sticky Bottom Input Area
			CommentInputArea(
				text = commentText,
				onValueChange = { commentText = it },
				onSend = {
					viewModel.addComment(postId, commentText)
					commentText = ""
				}
			)
		}
	}
}


@Composable
fun EmptyCommentsView() {
	Column(
		modifier = Modifier.fillMaxSize(),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = "No comments yet",
			style = MaterialTheme.typography.bodyLarge,
			color = Color.Gray
		)
		Text(
			text = "Be the first to start the conversation!",
			style = MaterialTheme.typography.bodySmall,
			color = Color.Gray
		)
	}
}