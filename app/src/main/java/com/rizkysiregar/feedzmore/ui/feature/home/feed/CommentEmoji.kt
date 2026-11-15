package com.rizkysiregar.feedzmore.ui.feature.home.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.rizkysiregar.feedzmore.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 15/11/26
 * Description: Software Engineer
 */

val QUICK_EMOJIS = listOf("😂", "❤️", "👍", "🔥", "😮", "😢", "🙏", "👏", "🎉", "😍")

@Composable
fun EmojiQuickRow(
	onEmojiClick: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	LazyRow(
		modifier = modifier,
		contentPadding = PaddingValues(horizontal = 12.dp),
		horizontalArrangement = Arrangement.spacedBy(4.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		items(QUICK_EMOJIS) { emoji ->
			Box(
				contentAlignment = Alignment.Center,
				modifier = Modifier
					.padding(vertical = 4.dp)
					.size(40.dp)
					.clip(CircleShape)
					.clickable(role = Role.Button, onClickLabel = stringResource(R.string.add_emoji, emoji)) { onEmojiClick(emoji) }
			) {
				Text(text = emoji, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
			}
		}
	}
}
