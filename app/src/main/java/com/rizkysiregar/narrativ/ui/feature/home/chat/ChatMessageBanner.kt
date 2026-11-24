package com.rizkysiregar.narrativ.ui.feature.home.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rizkysiregar.narrativ.R
import com.rizkysiregar.narrativ.ui.components.ProfileAvatar

/**
 * A message that just arrived in another conversation: slides in from the top, opens the conversation when tapped,
 * goes away when swiped up (or by itself, see [ChatBannerViewModel]).
 */
@Composable
fun ChatMessageBanner(
	banner: ChatBanner?,
	onOpen: (ChatBanner) -> Unit,
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier,
) {
	// the last banner stays composed while it slides out, so the exit animation has something to show
	var shown by remember { mutableStateOf(banner) }
	if (banner != null) shown = banner
	AnimatedVisibility(
		visible = banner != null,
		enter = slideInVertically { -it } + fadeIn(),
		exit = slideOutVertically { -it } + fadeOut(),
		modifier = modifier,
	) {
		val b = shown ?: return@AnimatedVisibility
		val body = b.text ?: stringResource(
			when (b.attachmentKind) {
				"image" -> R.string.chat_photo
				"video" -> R.string.chat_video
				else -> R.string.chat_file
			}
		)
		val title = if (b.groupName != null) stringResource(R.string.chat_banner_in_group, b.senderName, b.groupName) else b.senderName
		Surface(
			shape = RoundedCornerShape(18.dp),
			tonalElevation = 6.dp,
			shadowElevation = 8.dp,
			color = MaterialTheme.colorScheme.surfaceContainerHigh,
			modifier = Modifier
				.statusBarsPadding()
				.padding(horizontal = 12.dp, vertical = 8.dp)
				.widthIn(max = 560.dp)
				.fillMaxWidth()
				.semantics { liveRegion = LiveRegionMode.Polite }
				.pointerInput(b.messageId) {
					var dragged = 0f
					detectVerticalDragGestures(
						onDragStart = { dragged = 0f },
						onDragEnd = { if (dragged < -40f) onDismiss() },
						onVerticalDrag = { _, amount -> dragged += amount },
					)
				}
				.clickable(onClickLabel = stringResource(R.string.chat_banner_open)) { onOpen(b) },
		) {
			Row(
				modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp),
			) {
				ProfileAvatar(imageUrl = b.avatarUrl, userId = b.senderId, userName = b.senderName, size = 40.dp)
				Column(modifier = Modifier.weight(1f)) {
					Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
					Text(
						body,
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
					)
				}
			}
		}
	}
}
