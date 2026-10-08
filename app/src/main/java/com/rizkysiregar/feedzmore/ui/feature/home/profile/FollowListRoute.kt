package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun FollowListRoute(
	userId: String,
	type: String,
	onUserClick: (String, String) -> Unit,
	onBackClick: () -> Unit,
	viewModel: FollowListViewModel = hiltViewModel()
) {
	val items by viewModel.items.collectAsState()
	val isLoading by viewModel.isLoading.collectAsState()

	LaunchedEffect(userId, type) {
		viewModel.load(userId, type)
	}

	FollowListScreen(items = items, isLoading = isLoading, title = if (type == "followers") "Followers" else "Following", onUserClick = onUserClick, onBackClick = onBackClick)
}
