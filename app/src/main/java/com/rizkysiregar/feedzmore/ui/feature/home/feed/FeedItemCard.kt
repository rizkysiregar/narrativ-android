package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rizkysiregar.feedzmore.constants.Constants
import com.rizkysiregar.feedzmore.data.remote.model.response.FeedContentItem

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
			AsyncImage(
				model = Constants.BASE_IMAGE_URL + item.imageUrl,
				contentDescription = "Profile Picture",
				modifier = Modifier
					.size(42.dp)
					.clip(CircleShape),
//					.clickable { onUserClick(item) },
				contentScale = ContentScale.Crop
			)

			Spacer(Modifier.width(10.dp))

			// username
			Text(
				text = item.username,
				style = MaterialTheme.typography.bodyMedium.copy(
					fontWeight = FontWeight.Bold
				),
//				modifier = Modifier.clickable { onUserClick(item) }
			)
		}

		// --- Main Post Image ---
		AsyncImage(
			model = item.imageUrl,
			contentDescription = "Feed Image",
			modifier = Modifier
				.fillMaxWidth()
				.height(350.dp)
				.background(Color.LightGray),
			contentScale = ContentScale.Crop
		)

		Spacer(Modifier.height(8.dp))

		// --- Action (Like, comment, repost etc)
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			IconButton(
				onClick = { onLikeClick(item) },
			) {
				Icon(
					imageVector = if (item.liked) Icons.Filled.Favorite
					else Icons.Outlined.FavoriteBorder,
					contentDescription = "Like",
					tint = if (item.liked) Color.Red else MaterialTheme.colorScheme.onBackground
				)
			}

			Text(text = "${item.likeCount}")

			Spacer(modifier = Modifier.width(16.dp))

			IconButton(onClick = { onCommentClick(item) }) {
				Icon(
					imageVector = Icons.Outlined.MailOutline,
					contentDescription = "Comment"
				)
			}

			Text(text = "${item.commentCount}")
		}

		Spacer(modifier = Modifier.height(8.dp))

		// --- Caption ---
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 12.dp)
		) {
			Text(
				text = buildAnnotatedString {
					withStyle(
						style = SpanStyle(
							fontWeight = FontWeight.Bold
						)
					) {
						append(item.username)
					}
					append("  ")
					append(item.caption)
				},
				style = MaterialTheme.typography.bodyMedium
			)
			Spacer(Modifier.height(6.dp))
		}
	}
}


@Preview(showBackground = true)
@Composable
fun FeedItemCardPreview() {
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