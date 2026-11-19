package com.rizkysiregar.narrativ.ui.feature.home

import com.rizkysiregar.narrativ.R
import android.widget.Toast
import com.rizkysiregar.narrativ.ui.feature.home.settings.ChangeEmailRoute
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import com.rizkysiregar.narrativ.notification.PushNotifications
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizkysiregar.narrativ.constants.Constants
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.ui.components.PaperPlaneFlight
import com.rizkysiregar.narrativ.ui.feature.home.feed.FeedRoute
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostDetailHandoff
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostMediaViewerDestination
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostOpener
import com.rizkysiregar.narrativ.ui.feature.home.thread.PostThreadDestination
import com.rizkysiregar.narrativ.ui.feature.home.post.PostFeedRoute
import com.rizkysiregar.narrativ.ui.feature.home.profile.ProfileRoute
import com.rizkysiregar.narrativ.ui.feature.home.settings.SettingsRoute
import com.rizkysiregar.narrativ.ui.feature.moderation.BlockedUsersRoute
import com.rizkysiregar.narrativ.ui.feature.home.upload.UploadBannerViewModel
import com.rizkysiregar.narrativ.ui.feature.home.upload.UploadProgressBanner
import com.rizkysiregar.narrativ.ui.feature.home.userdetail.UserDetailRoute
import com.rizkysiregar.narrativ.ui.main.navigation.BottomNavScreen
import com.rizkysiregar.narrativ.ui.main.navigation.openUser
import com.rizkysiregar.narrativ.ui.main.navigation.routes.AuthPathRoutes
import com.rizkysiregar.narrativ.ui.main.navigation.routes.MainPathRoute
import com.rizkysiregar.narrativ.ui.main.navigation.routes.HomePathRoute

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 19/11/25
 * Description: Software Engineer
 */


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoot(
	navController: NavController
) {
	val homeNavController = rememberNavController()
	val uploadBannerViewModel: UploadBannerViewModel = hiltViewModel()
	val currentUserViewModel: CurrentUserViewModel = hiltViewModel()
	val currentUserId by currentUserViewModel.userId.collectAsState()
	val openUser: (String, String) -> Unit = { userId, username ->
		homeNavController.openUser(userId, username, currentUserId)
	}
	var feedRefreshVersion by remember { mutableIntStateOf(0) }
	// where the paper plane takes off from after a post; null while no plane is flying
	var planeOrigin by remember { mutableStateOf<Offset?>(null) }
	val currentRoute = homeNavController.currentBackStackEntryAsState().value?.destination?.route
	// What a list hands to the post detail. Not in the SavedStateHandle of the back stack: see PostDetailHandoff.
	val postHandoff: PostDetailHandoff = viewModel()
	val postOpener = remember(homeNavController, postHandoff) {
		object : PostOpener {
			override fun thread(post: FeedContentItem, onLike: (String) -> Unit, openComments: Boolean) {
				postHandoff.hand(post, onLike)
				homeNavController.navigate(HomePathRoute.PostDetail.create(post.id, openComments))
			}

			override fun media(post: FeedContentItem, onLike: (String) -> Unit, page: Int) {
				postHandoff.hand(post, onLike)
				homeNavController.navigate(HomePathRoute.PostMedia.create(post.id, page))
			}
		}
	}
	val isDetailRoute = currentRoute?.startsWith("post_detail/") == true || currentRoute?.startsWith("post_media/") == true
	// leaving the detail ends the hand-over, so a later visit (a push, say) never shows a stale post or like
	LaunchedEffect(isDetailRoute) { if (!isDetailRoute) postHandoff.clear() }

	// Ask once the user is in the app, not only when posting: a follower who never posts would otherwise
	// never get "new post" pushes.
	val context = LocalContext.current
	val notificationPermissionLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { }
	LaunchedEffect(Unit) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
			ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
			PackageManager.PERMISSION_GRANTED
		) {
			notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
		}
	}

	// A tapped push opens the post it is about; one without a post id (older pushes) opens the person's profile.
	val pushTarget by PushNotifications.pendingTarget.collectAsState()
	LaunchedEffect(pushTarget) {
		pushTarget?.let { target ->
			PushNotifications.clearPendingTarget()
			when (target) {
				is PushNotifications.PushTarget.Post ->
					homeNavController.navigate(HomePathRoute.PostDetail.create(target.postId, target.openComments, target.commentId))
				is PushNotifications.PushTarget.Profile ->
					openUser(target.userId, target.username)
			}
		}
	}

	LaunchedEffect(Unit) {
		uploadBannerViewModel.feedRefreshVersion.collect { feedRefreshVersion += 1 }
	}

	// the plane is drawn above the Scaffold so it keeps flying across the switch to the feed
	Box(modifier = Modifier.fillMaxSize()) {
	Scaffold(
		bottomBar = {
			if (currentRoute != HomePathRoute.UserDetail.route && currentRoute != HomePathRoute.FollowList.route && currentRoute != HomePathRoute.Settings.route && currentRoute != HomePathRoute.BlockedUsers.route && currentRoute != HomePathRoute.ChangeEmail.route && currentRoute != BottomNavScreen.PostFeed.route && !isDetailRoute) {
				BottomBar(homeNavController)
			}
		}
	) { paddingValues ->
		Column(modifier = Modifier.fillMaxSize()) {
			UploadProgressBanner(viewModel = uploadBannerViewModel)
			NavHost(
				navController = homeNavController,
				route = Constants.HOME_GRAPH,
				startDestination = BottomNavScreen.Feed.route,
				modifier = Modifier
					.padding(paddingValues)
					// the scaffold already pads for the system bars, so imePadding below must not count them again
					.consumeWindowInsets(paddingValues)
			) {
			composable(BottomNavScreen.Feed.route) {
				FeedRoute(
					refreshVersion = feedRefreshVersion,
					onCreateFeed = {
						homeNavController.navigate(BottomNavScreen.PostFeed.route) {
							launchSingleTop = true
						}
					},
					postOpener = postOpener,
					onUserClick = openUser,
					navController = homeNavController
				)
			}
				composable(BottomNavScreen.Explore.route) {
				com.rizkysiregar.narrativ.ui.feature.home.explore.ExploreRoute(
					postOpener = postOpener,
					onUserClick = openUser
				)
			}
			composable(BottomNavScreen.PostFeed.route) {
				PostFeedRoute(
					onClose = { homeNavController.popBackStack() },
					onPostSuccess = { origin ->
						planeOrigin = origin
						homeNavController.navigate(BottomNavScreen.Feed.route) {
							popUpTo(BottomNavScreen.PostFeed.route) {
								inclusive = true
							}
							launchSingleTop = true
						}
					}
				)
			}
			composable(BottomNavScreen.Profile.route) {
				ProfileRoute(
					onSettingsClick = {
						homeNavController.navigate(HomePathRoute.Settings.route)
					},
				onFollowersClick = { userId ->
					homeNavController.navigate(HomePathRoute.FollowList.create(userId, "followers"))
				},
				onFollowingClick = { userId ->
					homeNavController.navigate(HomePathRoute.FollowList.create(userId, "following"))
				},
				postOpener = postOpener
			)
		}
				composable(HomePathRoute.Settings.route) {
					SettingsRoute(
						onBackClick = { homeNavController.popBackStack() },
						onBlockedUsersClick = { homeNavController.navigate(HomePathRoute.BlockedUsers.route) },
						onChangeEmailClick = { homeNavController.navigate(HomePathRoute.ChangeEmail.route) },
						onLegalClick = { document -> navController.navigate(MainPathRoute.Legal.create(document.path)) },
						onLoggedOut = {
							navController.navigate(AuthPathRoutes.Login.route) {
								popUpTo(Constants.HOME_GRAPH) {
									inclusive = true
								}
								launchSingleTop = true
							}
						}
					)
				}
				composable(HomePathRoute.ChangeEmail.route) {
					val context = LocalContext.current
					ChangeEmailRoute(
						onBackClick = { homeNavController.popBackStack() },
						onChanged = {
							Toast.makeText(context, context.getString(R.string.email_changed), Toast.LENGTH_LONG).show()
							homeNavController.popBackStack()
						}
					)
				}
				composable(HomePathRoute.BlockedUsers.route) {
					BlockedUsersRoute(onBackClick = { homeNavController.popBackStack() })
				}
				composable(
					route = HomePathRoute.FollowList.route,
					arguments = listOf(
						navArgument(HomePathRoute.FollowList.ARG_USER_ID) { type = NavType.StringType },
						navArgument(HomePathRoute.FollowList.ARG_TYPE) { type = NavType.StringType }
					)
				) { entry ->
					com.rizkysiregar.narrativ.ui.feature.home.profile.FollowListRoute(
						userId = entry.arguments?.getString(HomePathRoute.FollowList.ARG_USER_ID).orEmpty(),
						type = entry.arguments?.getString(HomePathRoute.FollowList.ARG_TYPE).orEmpty(),
			onUserClick = openUser,
			onBackClick = { homeNavController.popBackStack() }
		)
			}
				composable(
					route = HomePathRoute.UserDetail.route,
				arguments = listOf(
					navArgument(HomePathRoute.UserDetail.ARG_USER_ID) { type = NavType.StringType },
					navArgument(HomePathRoute.UserDetail.ARG_USERNAME) { type = NavType.StringType }
				)
			) { entry ->
				UserDetailRoute(
					userId = entry.arguments?.getString(HomePathRoute.UserDetail.ARG_USER_ID).orEmpty(),
					username = entry.arguments?.getString(HomePathRoute.UserDetail.ARG_USERNAME).orEmpty(),
			onBackClick = { homeNavController.popBackStack() },
			onFollowersClick = { id ->
				homeNavController.navigate(HomePathRoute.FollowList.create(id, "followers"))
			},
			onFollowingClick = { id ->
				homeNavController.navigate(HomePathRoute.FollowList.create(id, "following"))
			},
			postOpener = postOpener
		)
		}
			composable(
				route = HomePathRoute.PostDetail.route,
				arguments = listOf(
					navArgument(HomePathRoute.PostDetail.ARG_POST_ID) { type = NavType.StringType },
					navArgument(HomePathRoute.PostDetail.ARG_OPEN_COMMENTS) {
						type = NavType.BoolType
						defaultValue = false
					},
					navArgument(HomePathRoute.PostDetail.ARG_COMMENT_ID) {
						type = NavType.StringType
						nullable = true
						defaultValue = null
					}
				)
			) { entry ->
				PostThreadDestination(
					postId = entry.arguments?.getString(HomePathRoute.PostDetail.ARG_POST_ID).orEmpty(),
					openComments = entry.arguments?.getBoolean(HomePathRoute.PostDetail.ARG_OPEN_COMMENTS) ?: false,
					focusCommentId = entry.arguments?.getString(HomePathRoute.PostDetail.ARG_COMMENT_ID),
					handoff = postHandoff,
					onBackClick = { homeNavController.popBackStack() },
					onUserClick = openUser,
					onMediaClick = { post, onLike, page -> postOpener.media(post, onLike, page) }
				)
			}
			composable(
				route = HomePathRoute.PostMedia.route,
				arguments = listOf(
					navArgument(HomePathRoute.PostMedia.ARG_POST_ID) { type = NavType.StringType },
					navArgument(HomePathRoute.PostMedia.ARG_PAGE) {
						type = NavType.IntType
						defaultValue = 0
					}
				)
			) { entry ->
				val postId = entry.arguments?.getString(HomePathRoute.PostMedia.ARG_POST_ID).orEmpty()
				PostMediaViewerDestination(
					navController = homeNavController,
					postId = postId,
					initialPage = entry.arguments?.getInt(HomePathRoute.PostMedia.ARG_PAGE) ?: 0,
					handoff = postHandoff,
					onCommentsClick = {
						// opened from the post's thread: go back to it rather than stacking a second one
						val previous = homeNavController.previousBackStackEntry
						val fromThread = previous?.destination?.route == HomePathRoute.PostDetail.route &&
							previous.arguments?.getString(HomePathRoute.PostDetail.ARG_POST_ID) == postId
						if (fromThread) {
							homeNavController.popBackStack()
						} else {
							homeNavController.navigate(HomePathRoute.PostDetail.create(postId, openComments = true))
						}
					}
				)
			}
		}
		}
	}
	planeOrigin?.let { origin ->
		PaperPlaneFlight(origin = origin, onFinished = { planeOrigin = null })
	}
	}
}
