package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.rizkysiregar.feedzmore.domain.post.PickedMedia
import com.rizkysiregar.feedzmore.ui.components.ProfileAvatar
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme
import com.rizkysiregar.feedzmore.ui.theme.Warning
import kotlinx.coroutines.delay

/** Warn about the caption limit once this few characters are left. */
private const val CAPTION_WARNING_THRESHOLD = 20
private val MediaThumbSize = 112.dp
private val CaptionMinHeight = 160.dp
private const val PROMPT_INTERVAL_MILLIS = 3500L
private val CaptionPrompts = listOf(
	"What's on your mind?",
	"Share something with your friends…",
	"What happened today?",
	"Got a story to tell?"
)

/**
 * Caption-first composer: the caption is the page, media is an optional extra that shows up
 * under it once picked. Media comes from the system photo picker, so the app needs no gallery permission.
 */
@Composable
fun PostFeedScreen(
	uiState: PostFeedUiState,
	isPreparing: Boolean,
	onCaptionChange: (String) -> Unit,
	onPickMedia: (List<Uri>) -> Unit,
	onRemoveMedia: (Int) -> Unit,
	onClose: () -> Unit,
	onNext: () -> Unit
) {
	val context = LocalContext.current
	var showDiscardDialog by remember { mutableStateOf(false) }

	val photoPickerLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_POST_MEDIA)
	) { uris -> if (uris.isNotEmpty()) onPickMedia(uris) }

	val notificationPermissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { }
	LaunchedEffect(Unit) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
			ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
			!= PackageManager.PERMISSION_GRANTED
		) {
			notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
		}
	}

	val requestClose = { if (uiState.hasContent) showDiscardDialog = true else onClose() }
	BackHandler(onBack = requestClose)

	val canAddMedia = uiState.selectedMedia.size < MAX_POST_MEDIA && !isPreparing

	val captionFocus = remember { FocusRequester() }
	// opens the keyboard straight away: writing is the point of this page
	LaunchedEffect(Unit) { captionFocus.requestFocus() }

	// a small buzz when the limit is hit, since further typing silently does nothing
	val haptic = LocalHapticFeedback.current
	val atLimit = uiState.caption.length >= MAX_CAPTION_LENGTH
	LaunchedEffect(atLimit) {
		if (atLimit) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.statusBarsPadding()
			.navigationBarsPadding()
			.imePadding()
	) {
		ComposerTopBar(
			canPost = uiState.canSubmit && !isPreparing,
			isPreparing = isPreparing,
			onClose = requestClose,
			onPost = onNext
		)
		if (isPreparing) {
			LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
		} else {
			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
		}

		Column(
			modifier = Modifier
				.weight(1f)
				.fillMaxWidth()
				.verticalScroll(rememberScrollState())
				// tapping anywhere in the empty area puts the cursor back in the caption
				.clickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null
				) { captionFocus.requestFocus() }
				.padding(horizontal = 16.dp, vertical = 12.dp)
		) {
			val author = uiState.author
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				ProfileAvatar(
					imageUrl = author?.profileImageUrl,
					userId = author?.id.orEmpty(),
					userName = author?.displayName().orEmpty(),
					size = 44.dp
				)
				Spacer(Modifier.width(12.dp))
				if (author != null) {
					Text(
						text = author.displayName(),
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.SemiBold
					)
				}
			}
			Spacer(Modifier.height(14.dp))
			CaptionCard(
				caption = uiState.caption,
				onCaptionChange = onCaptionChange,
				media = uiState.selectedMedia,
				onRemoveMedia = onRemoveMedia,
				focusRequester = captionFocus
			)
		}

		HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
		ComposerToolbar(
			caption = uiState.caption,
			mediaCount = uiState.selectedMedia.size,
			canAddMedia = canAddMedia,
			onPhotoClick = {
				photoPickerLauncher.launch(
					PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
				)
			},
			onVideoClick = {
				photoPickerLauncher.launch(
					PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
				)
			}
		)
	}

	if (showDiscardDialog) {
		AlertDialog(
			onDismissRequest = { showDiscardDialog = false },
			title = { Text("Discard post?") },
			text = { Text("Your caption and media will be lost.") },
			confirmButton = {
				TextButton(onClick = {
					showDiscardDialog = false
					onClose()
				}) { Text("Discard", color = MaterialTheme.colorScheme.error) }
			},
			dismissButton = {
				TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
			}
		)
	}
}

@Composable
private fun ComposerTopBar(
	canPost: Boolean,
	isPreparing: Boolean,
	onClose: () -> Unit,
	onPost: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(56.dp)
			.padding(horizontal = 4.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		IconButton(onClick = onClose) {
			Icon(Icons.Default.Close, contentDescription = "Close")
		}
		Spacer(Modifier.weight(1f))
		Button(
			onClick = onPost,
			// disabled while preparing, so a second tap cannot enqueue the post twice
			enabled = canPost,
			modifier = Modifier.padding(end = 8.dp),
			shape = RoundedCornerShape(20.dp)
		) {
			if (isPreparing) {
				CircularProgressIndicator(
					modifier = Modifier.size(18.dp),
					strokeWidth = 2.dp,
					color = MaterialTheme.colorScheme.onPrimary
				)
			} else {
				Text("Post", fontWeight = FontWeight.SemiBold)
			}
		}
	}
}

/**
 * The writing surface: a rounded card whose border lights up while typing. Picked media sits
 * inside the same card, so the caption and its attachments read as one post.
 */
@Composable
private fun CaptionCard(
	caption: String,
	onCaptionChange: (String) -> Unit,
	media: List<PickedMedia>,
	onRemoveMedia: (Int) -> Unit,
	focusRequester: FocusRequester
) {
	var focused by remember { mutableStateOf(false) }
	val borderColor by animateColorAsState(
		targetValue = if (focused) MaterialTheme.colorScheme.primary
		else MaterialTheme.colorScheme.outlineVariant,
		label = "caption-border"
	)
	val borderWidth by animateDpAsState(
		targetValue = if (focused) 1.5.dp else 1.dp,
		label = "caption-border-width"
	)
	val shape = RoundedCornerShape(22.dp)

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clip(shape)
			.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
			.border(borderWidth, borderColor, shape)
			.animateContentSize()
	) {
		TextField(
			value = caption,
			onValueChange = onCaptionChange,
			modifier = Modifier
				.fillMaxWidth()
				.heightIn(min = CaptionMinHeight)
				.focusRequester(focusRequester)
				.onFocusChanged { focused = it.isFocused },
			placeholder = { RotatingPlaceholder() },
			textStyle = MaterialTheme.typography.bodyLarge.copy(
				fontSize = 18.sp,
				lineHeight = 26.sp
			),
			keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
			colors = TextFieldDefaults.colors(
				focusedContainerColor = Color.Transparent,
				unfocusedContainerColor = Color.Transparent,
				disabledContainerColor = Color.Transparent,
				focusedIndicatorColor = Color.Transparent,
				unfocusedIndicatorColor = Color.Transparent,
				disabledIndicatorColor = Color.Transparent,
				cursorColor = MaterialTheme.colorScheme.primary
			)
		)
		AnimatedVisibility(visible = media.isNotEmpty()) {
			Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
				SelectedMediaStrip(media = media, onRemove = onRemoveMedia)
			}
		}
	}
}

/** Cycles through a few prompts while the caption is empty; it leaves composition once typing starts. */
@Composable
private fun RotatingPlaceholder() {
	var index by remember { mutableIntStateOf(0) }
	LaunchedEffect(Unit) {
		while (true) {
			delay(PROMPT_INTERVAL_MILLIS)
			index = (index + 1) % CaptionPrompts.size
		}
	}
	Crossfade(targetState = index, label = "caption-prompt") { current ->
		Text(
			text = CaptionPrompts[current],
			style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp)
		)
	}
}

@Composable
private fun SelectedMediaStrip(
	media: List<PickedMedia>,
	onRemove: (Int) -> Unit
) {
	LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
		itemsIndexed(media, key = { _, item -> item.uri.toString() }) { index, item ->
			val isVideo = item.mimeType?.startsWith("video/") == true
			Box(
				modifier = Modifier
					.size(MediaThumbSize)
					.clip(RoundedCornerShape(16.dp))
					.background(MaterialTheme.colorScheme.surfaceVariant)
			) {
				if (isVideo) {
					// Coil cannot draw a video frame here, so a video is shown as a play icon
					Icon(
						Icons.Default.PlayCircle,
						contentDescription = "Video ${index + 1}",
						modifier = Modifier
							.align(Alignment.Center)
							.size(36.dp)
					)
				} else {
					AsyncImage(
						model = item.uri,
						contentDescription = "Photo ${index + 1}",
						modifier = Modifier.fillMaxSize(),
						contentScale = ContentScale.Crop
					)
				}
				Surface(
					modifier = Modifier
						.align(Alignment.TopEnd)
						.padding(6.dp)
						.size(24.dp)
						.clickable { onRemove(index) },
					shape = CircleShape,
					color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
				) {
					Icon(
						Icons.Default.Close,
						contentDescription = "Remove media ${index + 1}",
						modifier = Modifier.padding(5.dp)
					)
				}
			}
		}
	}
}

/** Sits above the keyboard: add media on the left, counters on the right. */
@Composable
private fun ComposerToolbar(
	caption: String,
	mediaCount: Int,
	canAddMedia: Boolean,
	onPhotoClick: () -> Unit,
	onVideoClick: () -> Unit
) {
	val buttonColors = IconButtonDefaults.iconButtonColors(
		contentColor = MaterialTheme.colorScheme.primary
	)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 8.dp, vertical = 4.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		IconButton(onClick = onPhotoClick, enabled = canAddMedia, colors = buttonColors) {
			Icon(Icons.Outlined.Image, contentDescription = "Add photos")
		}
		IconButton(onClick = onVideoClick, enabled = canAddMedia, colors = buttonColors) {
			Icon(Icons.Outlined.Videocam, contentDescription = "Add a video")
		}
		Spacer(Modifier.weight(1f))
		if (mediaCount > 0) {
			Text(
				text = "$mediaCount/$MAX_POST_MEDIA media",
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Spacer(Modifier.width(12.dp))
		}
		CaptionCounter(length = caption.length, modifier = Modifier.padding(end = 8.dp))
	}
}

/** A ring that fills as the caption grows; the remaining count only appears near the limit. */
@Composable
private fun CaptionCounter(length: Int, modifier: Modifier = Modifier) {
	val remaining = MAX_CAPTION_LENGTH - length
	val nearLimit = remaining <= CAPTION_WARNING_THRESHOLD
	val progress by animateFloatAsState(
		targetValue = length / MAX_CAPTION_LENGTH.toFloat(),
		label = "caption-progress"
	)
	val color by animateColorAsState(
		targetValue = when {
			remaining <= 0 -> MaterialTheme.colorScheme.error
			nearLimit -> Warning
			else -> MaterialTheme.colorScheme.primary
		},
		label = "caption-counter-color"
	)

	AnimatedVisibility(visible = length > 0, modifier = modifier) {
		Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
			CircularProgressIndicator(
				progress = { progress },
				modifier = Modifier.fillMaxSize(),
				color = color,
				trackColor = MaterialTheme.colorScheme.outlineVariant,
				strokeWidth = 2.5.dp
			)
			if (nearLimit) {
				Text(
					text = "$remaining",
					style = MaterialTheme.typography.labelSmall,
					color = color
				)
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun PostFeedScreenPreview() {
	FeedzmoreTheme {
		PostFeedScreen(
			uiState = PostFeedUiState(
				caption = "Sunday morning ride",
				selectedMedia = listOf(PickedMedia("https://example.com/image.jpg".toUri(), "image/jpeg"))
			),
			isPreparing = false,
			onCaptionChange = {},
			onPickMedia = {},
			onRemoveMedia = {},
			onClose = {},
			onNext = {}
		)
	}
}
