package com.rizkysiregar.feedzmore.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.FeedContentItem

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
						} else {
							PostTileContent(post = post)
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
