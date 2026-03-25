package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rizkysiregar.feedzmore.data.remote.model.response.feed.CommentItem
import com.rizkysiregar.feedzmore.ui.components.InitialAvatar
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 08/03/26
 * Description: Software Engineer
 */

@Composable
fun CommentRow(comment: CommentItem) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(12.dp)
	) {
		// Profile picture Using initial avatar
		InitialAvatar(
			name = comment.username,
			size = 36.dp
		)

		Column(modifier = Modifier.weight(1f)) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = comment.username,
					style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
				)
				Spacer(modifier = Modifier.width(8.dp))
				Text(
					text = "2h", // You can format comment.createdAt here
					style = MaterialTheme.typography.bodySmall,
					color = Color.Gray
				)
			}
			Text(
				text = comment.commentContent,
				style = MaterialTheme.typography.bodyMedium,
				lineHeight = 20.sp
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun CommentRowPreview() {
	FeedzmoreTheme {
		CommentRow(
			comment = CommentItem(
				username = "John Doe",
				imageUrl = "https://example.com/image.jpg",
				commentContent = "This is a comment."
			)
		)
	}
}

