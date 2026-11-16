package com.rizkysiregar.narrativ.ui.feature.home.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rizkysiregar.narrativ.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.narrativ.ui.feature.home.feed.PostViewMode
import com.rizkysiregar.narrativ.ui.theme.NarrativTheme
import com.rizkysiregar.narrativ.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 22/11/25
 * Description: Software Engineer
 */

@Composable
fun ProfileRoute(
    profileVm: ProfileViewModel = hiltViewModel(),
    profileFeedVm: ProfileFeedViewModel = hiltViewModel(),
	onSettingsClick: () -> Unit,
	onFollowersClick: (String) -> Unit = {},
	onFollowingClick: (String) -> Unit = {},
	onPostClick: (FeedContentItem, (String) -> Unit) -> Unit = { _, _ -> }
) {
    val profileState by profileVm.profileState.collectAsState()
    val followStatus by profileVm.followStatus.collectAsState()
    val listFeed by profileFeedVm.uiState.collectAsStateWithLifecycle()
    val gridFeed by profileFeedVm.gridState.collectAsStateWithLifecycle()
    val isUpdatingPhoto by profileVm.isUpdatingPhoto.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var cropUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { cropUri = it }
    }
    val pagerState = rememberPagerState(initialPage = PostViewMode.GRID.ordinal) { PostViewMode.entries.size }

    // only fetch profile info, ProfileFeedViewModel handles its own init load
    LaunchedEffect(Unit) {
        profileVm.fetchUserProfile()
    }

    LaunchedEffect(Unit) {
        profileVm.photoMessages.collect { snackbarHostState.showSnackbar(it) }
    }

    cropUri?.let { picked ->
        Dialog(
            onDismissRequest = { cropUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            AvatarCropScreen(
                uri = picked,
                onCancel = { cropUri = null },
                onCropped = { file ->
                    cropUri = null
                    profileVm.onPhotoCropped(file)
                },
                onLoadFailed = {
                    cropUri = null
                    Toast.makeText(context, context.getString(R.string.error_photo_open), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    profileState?.let {
        ProfileScreen(
            profileState = it,
            pagerState = pagerState,
            gridFeed = gridFeed,
            listFeed = listFeed,
            onRetry = { profileVm.fetchUserProfile() },
            onRetryGrid = {
                if (gridFeed.hasLoadedInitialPage) profileFeedVm.loadNextGridPage()
                else profileFeedVm.refreshGrid()
            },
            onRetryList = {
                if (listFeed.hasLoadedInitialPage) profileFeedVm.loadNextPage()
                else profileFeedVm.refresh()
            },
            onLoadMoreGrid = profileFeedVm::loadNextGridPage,
            onLoadMoreList = profileFeedVm::loadNextPage,
            onRefresh = {
                profileVm.fetchUserProfile()
                profileFeedVm.refreshAll()
            },
            onLikeClick = profileFeedVm::toggleLike,
            onSettingsClick = onSettingsClick,
            followStatus = followStatus,
            onFollowersClick = { userId -> onFollowersClick(userId) },
            onFollowingClick = { userId -> onFollowingClick(userId) },
            onPostClick = { post -> onPostClick(post, profileFeedVm::toggleLike) },
            isUpdatingPhoto = isUpdatingPhoto,
            onChangePhoto = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemovePhoto = profileVm::removePhoto,
            snackbarHostState = snackbarHostState
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun ProfileRoutePreview() {
    NarrativTheme {
        ProfileRoute(
            onSettingsClick = {}
        )
    }
}
