package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.feedzmore.ui.components.InitialAvatar
import com.rizkysiregar.feedzmore.ui.components.PostGrid
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedUiState
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
	profileState: ResultState<UserSearchItemResponse>,
	feedState: FeedUiState,
	listState: LazyListState,
	onExitClick: () -> Unit,
	onRetry: () -> Unit,
	onRetryFeed: () -> Unit,
	onRefresh: () -> Unit,
	onLikeClick: (String) -> Unit,
	modifier: Modifier = Modifier,
) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Profile") },
				actions = {
					IconButton(onClick = onExitClick) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ExitToApp,
							contentDescription = "Exit App"
						)
					}
				},
				windowInsets = WindowInsets(0)
			)
		},
	) { innerPadding ->
		Box(
			modifier = modifier
				.fillMaxSize()
				.padding(innerPadding),
			contentAlignment = Alignment.Center
		) {
			when (profileState) {
				is ResultState.Loading -> {
					CircularProgressIndicator()
				}

				is ResultState.Success -> {
					val user = profileState.data
					LazyColumn(
						state = listState,
						modifier = Modifier
							.fillMaxSize()
					) {
						// item 1: User Profile information section
						item {
							UserProfileInfo(user = user)
						}

						// item 2: Post collection section
						item {
							Text(
								text = "My Posts",
								style = MaterialTheme.typography.titleMedium,
								modifier = Modifier
									.fillMaxWidth()
									.padding(horizontal = 16.dp, vertical = 8.dp)
							)
						}

						// User post section
						if (feedState.isError && feedState.items.isEmpty()) {
								item {
									Column(modifier = Modifier.padding(16.dp)) {
										Text("Could not load post.", color = MaterialTheme.colorScheme.error)
										Button(onClick = onRetryFeed) { Text("Retry") }
									}
							}
						} else {
							PostGrid(posts = feedState.items)
						}

							// Footer displays progress or a retry action for the current page.
							if (feedState.isLoadingMore || (feedState.isError && feedState.items.isNotEmpty())) {
								item {
									Box(
									modifier = Modifier
										.fillMaxWidth()
										.padding(16.dp),
									contentAlignment = Alignment.Center
								) {
										if (feedState.isLoadingMore) CircularProgressIndicator()
										else Button(onClick = onRetryFeed) { Text("Retry") }
									}
								}
						}
					}
				}

				is ResultState.Failure -> {
					Column(horizontalAlignment = Alignment.CenterHorizontally) {
						Text(
							text = "Error: ${profileState.message}",
							color = MaterialTheme.colorScheme.error
						)
						Button(onClick = onRetry) {
							Text("Retry")
						}
					}
				}

				is ResultState.Idle -> {
					// Initial state or nothing
				}
			}
		}
	}
}


@Composable
fun UserProfileInfo(
	user: UserSearchItemResponse,
) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.fillMaxWidth()
			.padding(16.dp)
	) {
		InitialAvatar(
			name = user.username,
			size = 120.dp
		)

		Text(
			text = user.displayName(),
			style = MaterialTheme.typography.headlineSmall,
			modifier = Modifier
				.padding(top = 16.dp)
		)
		Text(
			text = "@${user.username}",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}


@Preview(showSystemUi = true)
@Composable
private fun UserProfileInfoPreview() {
	UserProfileInfo(
		user = UserSearchItemResponse(
			id = "123",
			username = "rizkysiregar",
			firstName = "Rizky",
			lastName = "Siregar",
			email = "dumy@mail.com",
			profileImageUrl = "http://dummy.com",
		),
	)
}


//@Preview(showSystemUi = true)
//@Composable
//private fun ProfileScreenPreview() {
//	FeedzmoreTheme {
//		ProfileScreen(
//			profileState = ResultState.Success(
//				UserSearchItemResponse(
//					id = "123",
//					username = "rizkysiregar",
//					email = "habib@example.com",
//					firstName = "Rizky",
//					lastName = "Siregar",
//					fullName = "Rizky Siregar",
//					phoneNumber = "08123123123",
//					profileImageUrl = "https://dummy.com"
//				)
//			),
//			onRetry = {},
//			onExitClick = {},
//			modifier = Modifier,
//		)
//	}
//}
