package com.rizkysiregar.feedzmore.ui.feature.home.search

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.feedzmore.ui.components.ProfileAvatar
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 * Description: Software Engineer
 */

@Composable
fun SearchScreen(
	uiState: SearchUserUiState,
	onQueryChange: (String) -> Unit,
	onRetry: () -> Unit,
	onUserClick: (userId: String, username: String) -> Unit,
	listState: LazyListState = rememberLazyListState()
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
	) {
		SearchUserBar(
			query = uiState.query,
			onQueryChange = onQueryChange,
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 12.dp)
		)

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f)
		) {
			when {
				uiState.isIdle -> SearchIdleContent()

				uiState.isLoading -> Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center
				) {
					CircularProgressIndicator()
				}

				uiState.errorMessage != null && uiState.items.isEmpty() -> SearchErrorContent(
					message = uiState.errorMessage,
					onRetry = onRetry
				)

				uiState.hasLoadedInitialPage && uiState.items.isEmpty() -> SearchEmptyContent(
					query = uiState.query
				)

				else -> LazyColumn(
					state = listState,
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
					verticalArrangement = Arrangement.spacedBy(4.dp)
				) {
					items(
						items = uiState.items,
						key = { it.id },
						contentType = { "user_item" }
					) { user ->
						SearchUserItem(
							user = user,
							onClick = { onUserClick(user.id, user.username) }
						)
					}

					if (uiState.isLoadingMore) {
						item(contentType = "loading_more") {
							Box(
								modifier = Modifier
									.fillMaxWidth()
									.padding(16.dp),
								contentAlignment = Alignment.Center
							) {
								CircularProgressIndicator(modifier = Modifier.height(28.dp))
							}
						}
					}

					if (uiState.isError && uiState.items.isNotEmpty()) {
						item(contentType = "load_error") {
							Box(
								modifier = Modifier.fillMaxWidth(),
								contentAlignment = Alignment.Center
							) {
								Button(onClick = onRetry) { Text("Retry") }
							}
						}
					}
				}
			}
		}
	}
}

@Composable
private fun SearchUserBar(
	query: String,
	onQueryChange: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	OutlinedTextField(
		value = query,
		onValueChange = onQueryChange,
		modifier = modifier,
		singleLine = true,
		shape = RoundedCornerShape(24.dp),
		placeholder = { Text("Cari pengguna...") },
		leadingIcon = {
			Icon(
				imageVector = Icons.Default.Search,
				contentDescription = null
			)
		},
		trailingIcon = {
			if (query.isNotEmpty()) {
				IconButton(onClick = { onQueryChange("") }) {
					Icon(
						imageVector = Icons.Default.Close,
						contentDescription = "Bersihkan pencarian"
					)
				}
			}
		}
	)
}

@Composable
private fun SearchUserItem(
	user: UserSearchItemResponse,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		val displayName = user.displayName()

		ProfileAvatar(
			imageUrl = user.profileImageUrl,
			userId = user.id,
			userName = displayName,
			size = 48.dp
		)

		Spacer(modifier = Modifier.width(14.dp))

		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = displayName,
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
			Text(
				text = "@${user.username}",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}

@Composable
private fun SearchIdleContent() {
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
		Text("Cari pengguna")
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = "Ketik nama pengguna untuk mulai mencari",
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}

@Composable
private fun SearchEmptyContent(query: String) {
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
		Text("Tidak ada pengguna yang ditemukan")
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = "Tidak ada hasil untuk \"$query\"",
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}

@Composable
private fun SearchErrorContent(message: String, onRetry: () -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(text = message)
		Spacer(modifier = Modifier.height(8.dp))
		Button(onClick = onRetry) { Text("Retry") }
	}
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
	FeedzmoreTheme {
		SearchScreen(
			uiState = SearchUserUiState(
				query = "rizky",
				items = listOf(
					UserSearchItemResponse(
						id = "1",
						username = "rizkysiregar",
						firstName = "Habib Rizky",
						lastName = "Siregar",
						email = "rizky@example.com",
						profileImageUrl = null
					),
					UserSearchItemResponse(
						id = "2",
						username = "johndoe",
						firstName = null,
						lastName = null,
						email = null,
						profileImageUrl = null
					)
				),
				hasLoadedInitialPage = true
			),
			onQueryChange = {},
			onRetry = {},
			onUserClick = { _, _ -> }
		)
	}
}
