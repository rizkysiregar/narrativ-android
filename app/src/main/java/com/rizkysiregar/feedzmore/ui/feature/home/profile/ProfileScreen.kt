package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.UserProfileResponse
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.ui.components.InitialAvatar
import com.rizkysiregar.feedzmore.ui.components.ShimmerEffect
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
	profileState: ResultState<UserProfileResponse>,
	feedState: FeedUiState,
	onExitClick: () -> Unit,
	onRetry: () -> Unit,
	onLoadNextPage: () -> Unit, // pagination handler
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
						if (feedState.isError && feedState.items.isEmpty()) { // error or items empty
							item {
								Text(
									"Could not load post.",
									modifier = Modifier.padding(16.dp),
									color = MaterialTheme.colorScheme.error
								)
							}
						} else { // success show the list posts
							// --- START: MODIFIED GRID SECTION ---
							val posts = feedState.items
							val itemsPerRow = 3

							// Group the posts into chunks of 3 for each row
							val chunkedPosts = posts.chunked(itemsPerRow)
							items(chunkedPosts) { rowItems ->
								Row(
									modifier = Modifier
										.fillMaxWidth()
										.padding(horizontal = 16.dp), // Add horizontal padding to the row
									horizontalArrangement = Arrangement.spacedBy(4.dp) // Space between items
								) {
									// Create items in the row
									for (post in rowItems) {
										Surface(
											modifier = Modifier
												.weight(1f) // Each item takes equal space
												.aspectRatio(1f), // Make it a square
											shape = RectangleShape
										) {
											SubcomposeAsyncImage(
												model = ImageRequest.Builder(LocalContext.current)
													.data(post.imageUrl).build(),
												contentDescription = "Feed Image",
												modifier = Modifier
													.fillMaxSize(),
												contentScale = ContentScale.Crop,
												loading = {
													// Using the custom shimmer instead of Lottie
													ShimmerEffect(modifier = Modifier.fillMaxSize())
												},
												error = {
													Box(
														contentAlignment = Alignment.Center,
														modifier = Modifier
															.size(50.dp)
													) {
														Icon(
															imageVector = Icons.Outlined.Info,
															contentDescription = "Error",
														)
													}
												}
											)
										}
									}
									// Add invisible spacers to fill the row if it's not full
									// This ensures all rows have the same height
									val remainingItems = itemsPerRow - rowItems.size
									if (remainingItems > 0) {
										for (i in 0 until remainingItems) {
											Spacer(
												modifier = Modifier
													.weight(1f)
													.aspectRatio(1f)
											)
										}
									}
								}
								// Add vertical spacing between the rows
								Spacer(modifier = Modifier.height(4.dp))
							}
							// --- END: MODIFIED GRID SECTION ---
						}

						// Loading / Pagination Trigger
						item {
							if (feedState.isLoadingMore) {
								// Show loading spinner at the bottom
								Box(
									modifier = Modifier
										.fillMaxWidth()
										.padding(16.dp),
									contentAlignment = Alignment.Center
								) {
									CircularProgressIndicator()
								}
							} else {
								LaunchedEffect(Unit) {
									onLoadNextPage()
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
	user: UserProfileResponse,
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
			text = user.fullName,
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
		user = UserProfileResponse(
			"Rizky",
			"Siregar",
			"habib@example.com",
			"Rizky Siregar",
			"123",
			"http://dummy.com",
			"dumy@mail.com",
			"rizkysiregar",
		),
	)
}


@Preview(showSystemUi = true)
@Composable
private fun ProfileScreenPreview() {
	FeedzmoreTheme {
		ProfileScreen(
			profileState = ResultState.Success(
				UserProfileResponse(
					id = "123",
					username = "rizkysiregar",
					email = "habib@example.com",
					firstName = "Rizky",
					lastName = "Siregar",
					fullName = "Rizky Siregar",
					phoneNumber = "08123123123",
					profileImageUrl = "https://dummy.com"
				)
			),
			onRetry = {},
			onExitClick = {},
			onLoadNextPage = {},
			modifier = Modifier,
			feedState = FeedUiState(
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
		)
	}
}