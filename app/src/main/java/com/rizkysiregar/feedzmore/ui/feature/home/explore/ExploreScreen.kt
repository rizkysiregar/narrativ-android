package com.rizkysiregar.feedzmore.ui.feature.home.explore

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.ui.components.ShimmerEffect
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedItemCard
import com.rizkysiregar.feedzmore.ui.feature.home.feed.FeedUiState

@Composable
fun ExploreScreenContent(
	uiState: FeedUiState,
	gridState: LazyGridState,
	selectedPost: FeedContentItem?,
	onPostClick: (FeedContentItem) -> Unit,
	onDismissDetail: () -> Unit,
	onLikeClick: (String) -> Unit,
	onCommentClick: (FeedContentItem) -> Unit
) {
	val gridModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && selectedPost != null) {
		Modifier.fillMaxSize().blur(14.dp)
	} else {
		Modifier.fillMaxSize()
	}

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
	) {
		LazyVerticalGrid(
			state = gridState,
			columns = GridCells.Fixed(3),
			horizontalArrangement = Arrangement.spacedBy(2.dp),
			verticalArrangement = Arrangement.spacedBy(2.dp),
			modifier = gridModifier
		) {
			items(uiState.items, key = { it.id }) { post ->
				Surface(
					modifier = Modifier
						.fillMaxWidth()
						.aspectRatio(1f)
						.clickable { onPostClick(post) }
				) {
					SubcomposeAsyncImage(
						model = ImageRequest.Builder(LocalContext.current)
							.data(post.imageUrl).build(),
						contentDescription = "Post image",
						contentScale = ContentScale.Crop,
						modifier = Modifier.fillMaxSize(),
						loading = { ShimmerEffect(modifier = Modifier.fillMaxSize()) }
					)
				}
			}
		}

		if (selectedPost != null) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
					.clickable { onDismissDetail() },
				contentAlignment = Alignment.Center
			) {
				Surface(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp)
						.clickable(enabled = true) { },
					shape = RoundedCornerShape(20.dp),
					color = MaterialTheme.colorScheme.surface,
					tonalElevation = 6.dp,
					shadowElevation = 8.dp
				) {
				FeedItemCard(
					item = selectedPost,
					onLikeClick = onLikeClick,
					onCommentClick = onCommentClick
				)
				}
			}
		}
	}
}
