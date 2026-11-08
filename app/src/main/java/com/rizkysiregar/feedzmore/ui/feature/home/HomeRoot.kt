package com.rizkysiregar.feedzmore.ui.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.ui.feature.home.feed.KEY_LIKE_HANDLER
import com.rizkysiregar.feedzmore.ui.feature.home.feed.KEY_POST_JSON
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedRoute
import com.rizkysiregar.feedzmore.ui.feature.home.post.PostFeedRoute
import com.rizkysiregar.feedzmore.ui.feature.home.profile.ProfileRoute
import com.rizkysiregar.feedzmore.ui.feature.home.search.SearchRoute
import com.rizkysiregar.feedzmore.ui.feature.home.upload.UploadBannerViewModel
import com.rizkysiregar.feedzmore.ui.feature.home.upload.UploadProgressBanner
import com.rizkysiregar.feedzmore.ui.feature.home.userdetail.UserDetailRoute
import com.rizkysiregar.feedzmore.ui.main.navigation.BottomNavScreen
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.AuthPathRoutes
import com.rizkysiregar.feedzmore.ui.main.navigation.routes.HomePathRoute

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
	var feedRefreshVersion by remember { mutableIntStateOf(0) }
	val currentRoute = homeNavController.currentBackStackEntryAsState().value?.destination?.route
	val openPostDetail: (FeedContentItem, (String) -> Unit) -> Unit = { post, likeHandler ->
		homeNavController.currentBackStackEntry?.savedStateHandle?.apply {
			set(KEY_POST_JSON, Gson().toJson(post))
			set(KEY_LIKE_HANDLER, likeHandler)
		}
		homeNavController.navigate(HomePathRoute.PostDetail.create(post.id))
	}
	val isDetailRoute = currentRoute?.startsWith("post_detail/") == true

	LaunchedEffect(Unit) {
		uploadBannerViewModel.feedRefreshVersion.collect { feedRefreshVersion += 1 }
	}

	Scaffold(
		bottomBar = {
			if (currentRoute != HomePathRoute.UserDetail.route && currentRoute != HomePathRoute.FollowList.route && !isDetailRoute) {
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
				modifier = Modifier.padding(paddingValues)
			) {
			composable(BottomNavScreen.Feed.route) {
				FeedRoute(
					refreshVersion = feedRefreshVersion,
					onCreateFeed = {
						homeNavController.navigate(BottomNavScreen.PostFeed.route) {
							launchSingleTop = true
						}
					},
					navController = homeNavController
				)
			}
				composable(BottomNavScreen.Explore.route) {
				com.rizkysiregar.feedzmore.ui.feature.home.explore.ExploreRoute(
					navController = homeNavController
				)
			}
				composable(BottomNavScreen.Search.route) {
				SearchRoute(
					onUserClick = { userId, username ->
						homeNavController.navigate(HomePathRoute.UserDetail.create(userId, username))
					}
				)
			}
			composable(BottomNavScreen.PostFeed.route) {
				PostFeedRoute(
					onPostSuccess = {
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
					onLogoutClick = {
						navController.navigate(AuthPathRoutes.Login.route) {
							popUpTo(Constants.HOME_GRAPH) {
								inclusive = true
							}
							launchSingleTop = true
					}
				},
				onFollowersClick = { userId ->
					homeNavController.navigate(HomePathRoute.FollowList.create(userId, "followers"))
				},
				onFollowingClick = { userId ->
					homeNavController.navigate(HomePathRoute.FollowList.create(userId, "following"))
				},
				onPostClick = openPostDetail
			)
		}
				composable(
					route = HomePathRoute.FollowList.route,
					arguments = listOf(
						navArgument(HomePathRoute.FollowList.ARG_USER_ID) { type = NavType.StringType },
						navArgument(HomePathRoute.FollowList.ARG_TYPE) { type = NavType.StringType }
					)
				) { entry ->
					com.rizkysiregar.feedzmore.ui.feature.home.profile.FollowListRoute(
						userId = entry.arguments?.getString(HomePathRoute.FollowList.ARG_USER_ID).orEmpty(),
						type = entry.arguments?.getString(HomePathRoute.FollowList.ARG_TYPE).orEmpty(),
			onUserClick = { userId, username ->
				homeNavController.navigate(HomePathRoute.UserDetail.create(userId, username))
			},
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
			onPostClick = openPostDetail
		)
		}
			composable(
				route = HomePathRoute.PostDetail.route,
				arguments = listOf(
					navArgument(HomePathRoute.PostDetail.ARG_POST_ID) { type = NavType.StringType }
				)
			) {
				com.rizkysiregar.feedzmore.ui.feature.home.feed.PostDetailDestination(
					navController = homeNavController
				)
			}
		}
		}
	}
}
