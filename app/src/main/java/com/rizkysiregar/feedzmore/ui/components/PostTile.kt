package com.rizkysiregar.feedzmore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem

/**
 * Square grid cell of a post: the first media (a video through its thumbnail), a badge when the post
 * holds more than one, and the caption itself for a caption-only post. Taken down posts are the caller's job.
 */
@Composable
fun PostTileContent(post: FeedContentItem, modifier: Modifier = Modifier) {
	val first = post.mediaItems.firstOrNull()
	Box(modifier = modifier.fillMaxSize()) {
		when {
			first == null -> CaptionTile(caption = post.caption.orEmpty())

			// Coil cannot decode a video URL as an image: without a thumbnail show a placeholder
			// instead of a doomed network request.
			first.isVideo && first.thumbnailUrl == null -> Box(
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

			else -> SubcomposeAsyncImage(
				model = ImageRequest.Builder(LocalContext.current)
					.data(if (first.isVideo) first.thumbnailUrl else first.url.orEmpty())
					.build(),
				contentDescription = "Feed Image",
				modifier = Modifier.fillMaxSize(),
				contentScale = ContentScale.Crop,
				loading = { ShimmerEffect(modifier = Modifier.fillMaxSize()) },
				error = {
					Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
						Icon(imageVector = Icons.Outlined.Info, contentDescription = "Error")
					}
				}
			)
		}

		if (post.mediaItems.size > 1) {
			Icon(
				imageVector = Icons.Filled.Collections,
				contentDescription = "${post.mediaItems.size} media",
				tint = MaterialTheme.colorScheme.onBackground,
				modifier = Modifier
					.align(Alignment.TopEnd)
					.padding(6.dp)
					.size(18.dp)
			)
		}
	}
}

@Composable
private fun CaptionTile(caption: String) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surfaceVariant)
			.padding(8.dp),
		contentAlignment = Alignment.CenterStart
	) {
		Text(
			text = caption,
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			maxLines = 6,
			overflow = TextOverflow.Ellipsis
		)
	}
}
