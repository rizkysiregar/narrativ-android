package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.feedzmore.ui.components.ProfileAvatar
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedUiState
import com.rizkysiregar.feedzmore.ui.feature.home.feed.PostViewMode
import com.rizkysiregar.feedzmore.ui.feature.home.feed.PostViewPager
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme
import com.rizkysiregar.feedzmore.R
import androidx.compose.ui.res.stringResource

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ProfileScreen(
	profileState: ResultState<UserSearchItemResponse>,
	pagerState: PagerState,
	gridFeed: FeedUiState,
	listFeed: FeedUiState,
	onSettingsClick: () -> Unit,
	onRetry: () -> Unit,
	onRetryGrid: () -> Unit,
	onRetryList: () -> Unit,
	onLoadMoreGrid: () -> Unit,
	onLoadMoreList: () -> Unit,
	onRefresh: () -> Unit,
	onLikeClick: (String) -> Unit,
	followStatus: ResultState<com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse>?,
	onFollowersClick: (String) -> Unit,
	onFollowingClick: (String) -> Unit,
	onPostClick: (FeedContentItem) -> Unit = {},
	isUpdatingPhoto: Boolean = false,
	onChangePhoto: () -> Unit = {},
	onRemovePhoto: () -> Unit = {},
	snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
	modifier: Modifier = Modifier,
) {
	var showPhotoSheet by remember { mutableStateOf(false) }

	if (showPhotoSheet) {
		val hasPhoto = (profileState as? ResultState.Success)?.data?.profileImageUrl.isNullOrBlank().not()
		PhotoOptionsSheet(
			hasPhoto = hasPhoto,
			onChange = {
				showPhotoSheet = false
				onChangePhoto()
			},
			onRemove = {
				showPhotoSheet = false
				onRemovePhoto()
			},
			onDismiss = { showPhotoSheet = false }
		)
	}

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) },
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.profile)) },
				actions = {
					IconButton(onClick = onSettingsClick) {
						Icon(
							imageVector = Icons.Filled.Settings,
							contentDescription = stringResource(R.string.settings)
						)
					}
				},
				windowInsets = WindowInsets(0)
			)
		},
	) { innerPadding ->
		// the indicator follows the page on screen; a pull refreshes everything
		val isRefreshing = if (pagerState.currentPage == PostViewMode.GRID.ordinal) {
			gridFeed.isRefreshing
		} else {
			listFeed.isRefreshing
		}
		val pullState = rememberPullRefreshState(
			refreshing = isRefreshing,
			onRefresh = onRefresh
		)

		Box(
			modifier = modifier
				.fillMaxSize()
				.padding(innerPadding)
				.pullRefresh(pullState),
			contentAlignment = Alignment.Center
		) {
			when (profileState) {
				is ResultState.Loading -> {
					CircularProgressIndicator()
				}

				is ResultState.Success -> {
					val user = profileState.data
					PostViewPager(
						pagerState = pagerState,
						gridFeed = gridFeed,
						listFeed = listFeed,
						onGridPostClick = onPostClick,
						onLikeClick = onLikeClick,
						onVideoClick = onPostClick,
						onLoadMoreGrid = onLoadMoreGrid,
						onLoadMoreList = onLoadMoreList,
						onRetryGrid = onRetryGrid,
						onRetryList = onRetryList,
						modifier = Modifier.fillMaxSize(),
						header = {
							UserProfileInfo(
								user = user,
								followStatus = followStatus,
								onFollowersClick = { onFollowersClick(user.id) },
								onFollowingClick = { onFollowingClick(user.id) },
								isUpdatingPhoto = isUpdatingPhoto,
								onAvatarClick = { showPhotoSheet = true }
							)
						}
					)

					PullRefreshIndicator(
						refreshing = isRefreshing,
						state = pullState,
						modifier = Modifier.align(Alignment.TopCenter)
					)
				}

				is ResultState.Failure -> {
					Column(horizontalAlignment = Alignment.CenterHorizontally) {
						Text(
							text = stringResource(R.string.error_with_message, profileState.message),
							color = MaterialTheme.colorScheme.error
						)
						Button(onClick = onRetry) {
							Text(stringResource(R.string.retry))
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
	followStatus: ResultState<com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowStatusResponse>? = null,
	onFollowersClick: () -> Unit = {},
	onFollowingClick: () -> Unit = {},
	isUpdatingPhoto: Boolean = false,
	onAvatarClick: (() -> Unit)? = null,
) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.fillMaxWidth()
			.padding(16.dp)
	) {
		Box(
			contentAlignment = Alignment.Center,
			modifier = Modifier
				.clip(CircleShape)
				.then(
					if (onAvatarClick != null && !isUpdatingPhoto) Modifier.clickable(onClick = onAvatarClick)
					else Modifier
				)
		) {
			ProfileAvatar(
				imageUrl = user.profileImageUrl,
				userId = user.id,
				userName = user.displayName(),
				size = 120.dp
			)
			if (isUpdatingPhoto) {
				Box(
					contentAlignment = Alignment.Center,
					modifier = Modifier
						.size(120.dp)
						.background(Color.Black.copy(alpha = 0.45f))
				) {
					CircularProgressIndicator(color = Color.White)
				}
			}
		}
		if (onAvatarClick != null) {
			Text(
				text = stringResource(R.string.change_photo),
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier.padding(top = 8.dp)
			)
		}

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

		if (followStatus is ResultState.Success) {
			val status = followStatus.data
			androidx.compose.foundation.layout.Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp),
				horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
			) {
				FollowMetric(count = status.followerCount, label = stringResource(R.string.followers), onClick = onFollowersClick)
				FollowMetric(count = status.followingCount, label = stringResource(R.string.following), onClick = onFollowingClick)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoOptionsSheet(
	hasPhoto: Boolean,
	onChange: () -> Unit,
	onRemove: () -> Unit,
	onDismiss: () -> Unit
) {
	ModalBottomSheet(onDismissRequest = onDismiss) {
		ListItem(
			headlineContent = { Text(if (hasPhoto) stringResource(R.string.replace_photo) else stringResource(R.string.choose_photo)) },
			leadingContent = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
			modifier = Modifier.clickable(onClick = onChange)
		)
		if (hasPhoto) {
			ListItem(
				headlineContent = { Text(stringResource(R.string.remove_photo), color = MaterialTheme.colorScheme.error) },
				leadingContent = {
					Icon(
						Icons.Filled.Delete,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.error
					)
				},
				modifier = Modifier.clickable(onClick = onRemove)
			)
		}
		Spacer(modifier = Modifier.height(24.dp))
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
			fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
		)
		Text(
			text = label,
			style = MaterialTheme.typography.bodySmall,
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
//			onSettingsClick = {},
//			modifier = Modifier,
//		)
//	}
//}
