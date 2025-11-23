package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.data.remote.model.response.FeedContentItem
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreandroidnativeTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 23/11/25
 * Description: Software Engineer
 */


@Composable
fun FeedItemCard(
	item: FeedContentItem,
	onLikeClick: (FeedContentItem) -> Unit = {},
	onCommentClick: (FeedContentItem) -> Unit = {},
	onUserClick: (FeedContentItem) -> Unit = {},
) {

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.background(MaterialTheme.colorScheme.background)
	) {
		// --- Top User Bar ----
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 12.dp, vertical = 10.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			// Profile picture


		}
	}

}


@Preview(showBackground = true)
@Composable
fun FeedItemCardPreview() {
	FeedzmoreandroidnativeTheme {
		FeedItemCard(
			item = FeedContentItem(
				id = "1",
				caption = "this is caption",
				commentCount = 1,
				likeCount = 1,
				liked = true,
				createdAt = "2023-11-25",
				imageUrl = "https://dummy-url",
				username = "John Doe"
			)
		)
	}
}