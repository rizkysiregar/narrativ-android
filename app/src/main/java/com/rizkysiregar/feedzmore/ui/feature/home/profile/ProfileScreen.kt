package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.UserProfileResponse
import com.rizkysiregar.feedzmore.ui.components.ProfileAvatar
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreandroidnativeTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
	profileState: ResultState<UserProfileResponse>,
	onExitClick: () -> Unit,
	onRetry: () -> Unit,
	modifier: Modifier = Modifier,
	userId: String
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
						modifier = Modifier.fillMaxSize(),
					) {
						// item 1: User Profile information section
						item {
							UserProfileInfo(user = user, userId = userId)
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


						// item 3: The Post Grid.
						// This is NOT a LazyVerticalGrid. It's a regular grid
						// that lives inside the main LazyColumn.
						// We achieve this by creating a grid layout manually with Rows.
						val items = (1..20).toList() // Your list of posts
						val columns = 3
						val rows = (items.size + columns - 1) / columns // Calculate rows needed

						items(rows) { rowIndex ->
							Row(
								modifier = Modifier
									.fillMaxWidth()
									.padding(horizontal = 12.dp), // Add padding for spacing
								horizontalArrangement = Arrangement.spacedBy(4.dp)
							) {
								for (columnIndex in 0 until columns) {
									val itemIndex = rowIndex * columns + columnIndex
									if (itemIndex < items.size) {
										Surface(
											modifier = Modifier
												.weight(1f) // Each item takes equal space in the row
												.aspectRatio(1f), // Makes it a square
											shape = MaterialTheme.shapes.small,
											color = MaterialTheme.colorScheme.surfaceVariant
										) {
											Box(contentAlignment = Alignment.Center) {
												Text("Post $itemIndex")
											}
										}
									} else {
										// Add an empty Spacer for incomplete rows
										Box(modifier = Modifier.weight(1f))
									}
								}
							}
							// Add vertical spacing between rows
							if (rowIndex < rows - 1) {
								androidx.compose.foundation.layout.Spacer(
									modifier = Modifier.padding(
										2.dp
									)
								)
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
	userId: String
) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.fillMaxWidth()
			.padding(16.dp)
	) {
		ProfileAvatar(
			null,
			userId = userId,
			userName = user.fullName,
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
		userId = "userId"
	)
}


@Preview(showSystemUi = true)
@Composable
private fun ProfileScreenPreview() {
	FeedzmoreandroidnativeTheme {
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
			), onRetry = {},
			onExitClick = {},
			modifier = Modifier,
			userId = "userId"
		)
	}
}