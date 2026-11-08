package com.rizkysiregar.feedzmore.ui.feature.home.userdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.data.remote.model.ResultState
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.feedzmore.ui.components.PostGrid
import com.rizkysiregar.feedzmore.ui.components.ProfileAvatar
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedUiState
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 * Description: Software Engineer
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
	profileState: ResultState<UserSearchItemResponse>,
	feedState: FeedUiState,
	listState: LazyListState = rememberLazyListState(),
	onBackClick: () -> Unit,
	onRetry: () -> Unit,
	onRetryFeed: () -> Unit,
	followStatus: ResultState<com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse>? = null,
	isFollowActionLoading: Boolean = false,
	onFollowClick: () -> Unit = {},
	onFollowersClick: () -> Unit = {},
	onFollowingClick: () -> Unit = {},
	onPostClick: (FeedContentItem) -> Unit = {}
) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Detail Pengguna") },
				navigationIcon = {
					IconButton(onClick = onBackClick) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "Kembali"
						)
					}
				},
				windowInsets = WindowInsets(0)
			)
		},
	) { innerPadding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)),
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
						modifier = Modifier.fillMaxSize()
					) {
					item { UserDetailHeader(user = user) }
					item {
						FollowStatusSection(
							followStatus = followStatus,
							isFollowActionLoading = isFollowActionLoading,
							onFollowClick = onFollowClick,
							onFollowersClick = onFollowersClick,
							onFollowingClick = onFollowingClick
						)
					}
					item { UserInfoCard(user = user) }
						item {
							Text(
								text = "Posts",
								style = MaterialTheme.typography.titleMedium,
								modifier = Modifier
									.fillMaxWidth()
									.padding(horizontal = 16.dp, vertical = 8.dp)
							)
						}

						if (feedState.isError && feedState.items.isEmpty()) {
							item {
								Column(modifier = Modifier.padding(16.dp)) {
									Text("Could not load post.", color = MaterialTheme.colorScheme.error)
									Button(onClick = onRetryFeed) { Text("Retry") }
								}
							}
						} else {
							if (!feedState.hasLoadedInitialPage) {
								item {
									Box(
										modifier = Modifier
											.fillMaxWidth()
											.padding(16.dp),
										contentAlignment = Alignment.Center
									) {
										CircularProgressIndicator()
									}
								}
							} else if (feedState.items.isEmpty()) {
								item {
									Text(
										text = "Belum ada posts",
										color = MaterialTheme.colorScheme.onSurfaceVariant,
										modifier = Modifier
											.fillMaxWidth()
											.padding(horizontal = 16.dp, vertical = 8.dp)
									)
								}
							}
							PostGrid(posts = feedState.items, onPostClick = onPostClick)
						}

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
						Button(onClick = onRetry) { Text("Retry") }
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
private fun UserDetailHeader(user: UserSearchItemResponse) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.fillMaxWidth()
			.padding(16.dp)
	) {
		ProfileAvatar(
			imageUrl = user.profileImageUrl,
			userId = user.id,
			userName = user.displayName(),
			size = 120.dp
		)
		Text(
			text = user.displayName(),
			style = MaterialTheme.typography.headlineSmall,
			fontWeight = FontWeight.Bold,
			modifier = Modifier.padding(top = 16.dp)
		)
		Text(
			text = "@${user.username}",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}

@Composable
private fun FollowStatusSection(
	followStatus: ResultState<com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse>?,
	isFollowActionLoading: Boolean,
	onFollowClick: () -> Unit,
	onFollowersClick: () -> Unit,
	onFollowingClick: () -> Unit
) {
	Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
		if (followStatus is ResultState.Success) {
			val status = followStatus.data
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
			) {
				FollowMetric(count = status.followerCount, label = "Followers", onClick = onFollowersClick)
				FollowMetric(count = status.followingCount, label = "Following", onClick = onFollowingClick)
			}
			Spacer(modifier = Modifier.height(12.dp))
			Button(
				onClick = onFollowClick,
				enabled = !isFollowActionLoading,
				modifier = Modifier.fillMaxWidth(),
				colors = if (status.isFollowing) androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
				else androidx.compose.material3.ButtonDefaults.buttonColors()
			) {
				if (isFollowActionLoading) {
					CircularProgressIndicator(modifier = Modifier.size(18.dp))
				} else {
					Text(if (status.isFollowing) "Unfollow" else "Follow")
				}
			}
		}
	}
}

@Composable
private fun FollowMetric(count: Long, label: String, onClick: () -> Unit) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.padding(horizontal = 20.dp)
			.clickable(onClick = onClick)
	) {
		Text(
			text = count.toString(),
			style = MaterialTheme.typography.titleLarge,
			fontWeight = FontWeight.Bold
		)
		Text(
			text = label,
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}

@Composable
private fun UserInfoCard(user: UserSearchItemResponse) {
	val email = user.email?.trim().orEmpty()
	val phone = user.phoneNumber?.trim().orEmpty()

	if (email.isEmpty() && phone.isEmpty()) return

	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp),
		shape = RoundedCornerShape(16.dp),
		color = MaterialTheme.colorScheme.surface
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			if (email.isNotEmpty()) {
				UserInfoRow(icon = Icons.Default.Email, label = "Email", value = email)
			}
			if (email.isNotEmpty() && phone.isNotEmpty()) {
				Spacer(modifier = Modifier.height(12.dp))
			}
			if (phone.isNotEmpty()) {
				UserInfoRow(icon = Icons.Default.Phone, label = "Nomor telepon", value = phone)
			}
		}
	}
}

@Composable
private fun UserInfoRow(
	icon: ImageVector,
	label: String,
	value: String
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			modifier = Modifier.size(20.dp),
			tint = MaterialTheme.colorScheme.primary
		)
		Spacer(modifier = Modifier.width(12.dp))
		Column {
			Text(
				text = label,
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Text(
				text = value,
				style = MaterialTheme.typography.bodyLarge
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
fun UserDetailScreenPreview() {
	FeedzmoreTheme {
		UserDetailScreen(
			profileState = ResultState.Success(
				UserSearchItemResponse(
					id = "1",
					username = "rizkysiregar",
					firstName = "Habib Rizky",
					lastName = "Siregar",
					email = "rizky@example.com",
					phoneNumber = "081234567890"
				)
			),
			feedState = FeedUiState(
				items = List(6) { index ->
					FeedContentItem(
						id = index.toString(),
						createdAt = "2026-10-07",
						mediaUrl = "https://dummy-url",
					mediaType = com.rizkysiregar.feedzmore.data.remote.model.response.feed.MediaType.IMAGE,
					thumbnailUrl = null,
						caption = "caption $index",
						likeCount = 1,
						commentCount = 1,
						liked = false,
						username = "rizkysiregar"
					)
				},
				hasLoadedInitialPage = true
			),
			onBackClick = {},
			onRetry = {},
			onRetryFeed = {}
		)
	}
}
