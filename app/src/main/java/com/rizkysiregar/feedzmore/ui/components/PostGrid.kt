package com.rizkysiregar.feedzmore.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.MediaType

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/10/26
 * Description: Software Engineer
 */

fun LazyListScope.PostGrid(
	posts: List<FeedContentItem>,
	itemsPerRow: Int = 3,
	onPostClick: (FeedContentItem) -> Unit = {}
) {
	items(posts.chunked(itemsPerRow)) { rowItems ->
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp),
			horizontalArrangement = Arrangement.spacedBy(4.dp)
		) {
				for (post in rowItems) {
					Surface(
						modifier = Modifier
							.weight(1f)
							.aspectRatio(1f)
							.then(
								if (!post.isTakenDown) {
									Modifier.clickable { onPostClick(post) }
								} else {
									Modifier
								}
							),
						shape = RectangleShape
					) {
						if (post.isTakenDown) {
							TakenDownPlaceholder(
								message = post.takedownMessage,
								modifier = Modifier.fillMaxSize(),
								compact = true
							)
						} else if (post.mediaType == MediaType.VIDEO && post.thumbnailUrl == null) {
							// Coil cannot decode a video URL as an image — show a
							// placeholder instead of a doomed network request.
							Box(
								modifier = Modifier
									.fillMaxSize()
									.background(MaterialTheme.colorScheme.surfaceVariant),
								contentAlignment = Alignment.Center
							) {
								Icon(
									imageVector = Icons.Filled.PlayCircle,
									contentDescription = "Video",
									tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
									modifier = Modifier.size(32.dp)
								)
							}
						} else {
							SubcomposeAsyncImage(
								model = ImageRequest.Builder(LocalContext.current)
									.data(
										if (post.mediaType == MediaType.VIDEO) post.thumbnailUrl
										else post.mediaUrl.orEmpty()
									).build(),
								contentDescription = "Feed Image",
								modifier = Modifier.fillMaxSize(),
								contentScale = ContentScale.Crop,
								loading = {
									ShimmerEffect(modifier = Modifier.fillMaxSize())
								},
								error = {
									Box(
										contentAlignment = Alignment.Center,
										modifier = Modifier.size(50.dp)
									) {
										Icon(
											imageVector = Icons.Outlined.Info,
											contentDescription = "Error",
										)
									}
								}
							)
						}
					}
				}

			val remainingItems = itemsPerRow - rowItems.size
			if (remainingItems > 0) {
				repeat(remainingItems) {
					Spacer(
						modifier = Modifier
							.weight(1f)
							.aspectRatio(1f)
					)
				}
			}
		}
		Spacer(modifier = Modifier.height(4.dp))
	}
}
