package com.rizkysiregar.narrativ.ui.feature.home.userdetail

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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import com.rizkysiregar.narrativ.data.remote.model.ResultState
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.data.remote.model.response.user.UserSearchItemResponse
import com.rizkysiregar.narrativ.domain.moderation.ReportTarget
import com.rizkysiregar.narrativ.ui.feature.moderation.ContentModerationMenu
import com.rizkysiregar.narrativ.ui.feature.moderation.ModerationTarget
import com.rizkysiregar.narrativ.ui.components.ProfileAvatar
import com.rizkysiregar.narrativ.ui.feature.home.feed.FeedUiState
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostViewPager
import com.rizkysiregar.narrativ.ui.theme.NarrativTheme
import com.rizkysiregar.narrativ.R
import androidx.compose.ui.res.stringResource

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 * Description: Software Engineer
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
	profileState: ResultState<UserSearchItemResponse>,
	pagerState: PagerState,
	gridFeed: FeedUiState,
	listFeed: FeedUiState,
	onBackClick: () -> Unit,
	onRetry: () -> Unit,
	onRetryGrid: () -> Unit,
	onRetryList: () -> Unit,
	onLoadMoreGrid: () -> Unit,
	onLoadMoreList: () -> Unit,
	onLikeClick: (String) -> Unit,
	followStatus: ResultState<com.rizkysiregar.narrativ.data.remote.model.response.user.FollowStatusResponse>? = null,
	isFollowActionLoading: Boolean = false,
	// your own account: nothing to follow, so no Follow button
	isOwnProfile: Boolean = false,
	onFollowClick: () -> Unit = {},
	onFollowersClick: () -> Unit = {},
	onFollowingClick: () -> Unit = {},
	onPostClick: (FeedContentItem) -> Unit = {},
	userId: String? = null,
	username: String? = null,
	// set where the profile may be reported or the user blocked; runs after the user was blocked
	onUserBlocked: (() -> Unit)? = null
) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.user_detail)) },
				navigationIcon = {
					IconButton(onClick = onBackClick) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
						)
					}
				},
				actions = {
					if (onUserBlocked != null && userId != null && username != null) {
						ContentModerationMenu(
							target = ModerationTarget(ReportTarget.USER, userId, userId, username),
							onBlocked = onUserBlocked
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
							Column {
								UserDetailHeader(user = user)
								FollowStatusSection(
									followStatus = followStatus,
									isFollowActionLoading = isFollowActionLoading,
									showFollowButton = !isOwnProfile,
									onFollowClick = onFollowClick,
									onFollowersClick = onFollowersClick,
									onFollowingClick = onFollowingClick
								)
								UserInfoCard(user = user)
							}
						}
					)
				}

				is ResultState.Failure -> {
					Column(horizontalAlignment = Alignment.CenterHorizontally) {
						Text(
							text = stringResource(R.string.error_with_message, profileState.message),
							color = MaterialTheme.colorScheme.error
						)
						Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
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
	followStatus: ResultState<com.rizkysiregar.narrativ.data.remote.model.response.user.FollowStatusResponse>?,
	isFollowActionLoading: Boolean,
	showFollowButton: Boolean,
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
				FollowMetric(count = status.followerCount, label = stringResource(R.string.followers), onClick = onFollowersClick)
				FollowMetric(count = status.followingCount, label = stringResource(R.string.following), onClick = onFollowingClick)
			}
			if (!showFollowButton) return@Column
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
					Text(if (status.isFollowing) stringResource(R.string.unfollow) else stringResource(R.string.follow))
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
				UserInfoRow(icon = Icons.Default.Email, label = stringResource(R.string.email), value = email)
			}
			if (email.isNotEmpty() && phone.isNotEmpty()) {
				Spacer(modifier = Modifier.height(12.dp))
			}
			if (phone.isNotEmpty()) {
				UserInfoRow(icon = Icons.Default.Phone, label = stringResource(R.string.phone_number), value = phone)
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
	val previewFeed = FeedUiState(
		items = List(6) { index ->
			FeedContentItem(
				id = index.toString(),
				createdAt = "2026-10-07",
				media = listOf(
					com.rizkysiregar.narrativ.data.remote.model.response.feed.PostMediaItem(
						"m$index",
						com.rizkysiregar.narrativ.data.remote.model.response.feed.MediaType.IMAGE,
						"https://dummy-url",
						null
					)
				),
				caption = "caption $index",
				likeCount = 1,
				commentCount = 1,
				liked = false,
				username = "rizkysiregar"
			)
		},
		hasLoadedInitialPage = true
	)
	NarrativTheme {
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
			pagerState = rememberPagerState { 2 },
			gridFeed = previewFeed,
			listFeed = previewFeed,
			onBackClick = {},
			onRetry = {},
			onRetryGrid = {},
			onRetryList = {},
			onLoadMoreGrid = {},
			onLoadMoreList = {},
			onLikeClick = {}
		)
	}
}
