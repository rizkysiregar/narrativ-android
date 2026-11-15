package com.rizkysiregar.feedzmore.ui.feature.home.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.data.remote.model.response.user.FollowUserResponse
import com.rizkysiregar.feedzmore.ui.components.ProfileAvatar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.rizkysiregar.feedzmore.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
	items: List<FollowUserResponse>,
	isLoading: Boolean,
	title: String,
	onUserClick: (String, String) -> Unit,
	onBackClick: () -> Unit
) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(title) },
				navigationIcon = {
					IconButton(onClick = onBackClick) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
					}
				}
			)
		}
	) { padding ->
	Box(modifier = Modifier.fillMaxSize().padding(padding)) {
		LazyColumn {
			items(items, key = { it.id }) { user ->
				ListItem(
					headlineContent = { Text(user.displayName()) },
					supportingContent = { Text("@${user.username}") },
					leadingContent = {
						ProfileAvatar(
							imageUrl = user.profileImage,
							userId = "", // no photo: initials, like the rest of the feed
							userName = user.username,
							size = 44.dp
						)
					},
					colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
					modifier = Modifier
						.fillMaxWidth()
						.clickable { onUserClick(user.id, user.username) }
				)
			}
		}
		if (isLoading && items.isEmpty()) {
			CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
		}
	}
	}
}
