package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.rizkysiregar.feedzmore.domain.post.PickedMedia
import com.rizkysiregar.feedzmore.ui.components.ProfileAvatar
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/** Warn about the caption limit once this few characters are left. */
private const val CAPTION_WARNING_THRESHOLD = 20
private val MediaThumbSize = 112.dp

/**
 * Caption-first composer: the caption is the page, media is an optional extra that shows up
 * under it once picked. The gallery lives in a sheet so it never competes with the caption.
 */
@Composable
fun PostFeedScreen(
	uiState: PostFeedUiState,
	isPreparing: Boolean,
	onCaptionChange: (String) -> Unit,
	onPickMedia: (List<Uri>) -> Unit,
	onToggleRecentImage: (Uri) -> Unit,
	onRemoveMedia: (Int) -> Unit,
	onLoadRecentImages: () -> Unit,
	onClose: () -> Unit,
	onNext: () -> Unit
) {
	val context = LocalContext.current
	val galleryPermission = remember {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			Manifest.permission.READ_MEDIA_IMAGES
		} else {
			Manifest.permission.READ_EXTERNAL_STORAGE
		}
	}
	var hasGalleryAccess by remember {
		mutableStateOf(
			ContextCompat.checkSelfPermission(context, galleryPermission) == PackageManager.PERMISSION_GRANTED
		)
	}
	var showGallery by remember { mutableStateOf(false) }
	var showDiscardDialog by remember { mutableStateOf(false) }

	val photoPickerLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_POST_MEDIA)
	) { uris -> if (uris.isNotEmpty()) onPickMedia(uris) }
	val permissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { granted -> hasGalleryAccess = granted }

	// the gallery permission is only asked for once the user opens the gallery
	LaunchedEffect(showGallery, hasGalleryAccess) {
		if (!showGallery) return@LaunchedEffect
		if (hasGalleryAccess) onLoadRecentImages()
		else permissionLauncher.launch(galleryPermission)
	}

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
				.padding(horizontal = 16.dp, vertical = 12.dp)
		) {
			Row(modifier = Modifier.fillMaxWidth()) {
				val author = uiState.author
				ProfileAvatar(
					imageUrl = author?.profileImageUrl,
					userId = author?.id.orEmpty(),
					userName = author?.displayName().orEmpty(),
					size = 44.dp
				)
				Spacer(Modifier.width(12.dp))
				Column(modifier = Modifier.weight(1f)) {
					if (author != null) {
						Text(
							text = author.displayName(),
							style = MaterialTheme.typography.titleMedium,
							fontWeight = FontWeight.SemiBold
						)
					}
					CaptionField(caption = uiState.caption, onCaptionChange = onCaptionChange)
					if (uiState.selectedMedia.isNotEmpty()) {
						Spacer(Modifier.height(12.dp))
						SelectedMediaStrip(
							media = uiState.selectedMedia,
							onRemove = onRemoveMedia
						)
					}
				}
			}
		}

		HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
		ComposerToolbar(
			caption = uiState.caption,
			mediaCount = uiState.selectedMedia.size,
			canAddMedia = canAddMedia,
			onGalleryClick = { showGallery = true },
			onVideoClick = {
				photoPickerLauncher.launch(
					PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
				)
			}
		)
	}

	if (showGallery) {
		GallerySheet(
			images = uiState.recentImages,
			selectedImages = uiState.selectedMedia.map { it.uri }.toSet(),
			selectedCount = uiState.selectedMedia.size,
			isLoading = uiState.isLoadingGallery,
			hasGalleryAccess = hasGalleryAccess,
			onImageToggled = onToggleRecentImage,
			onBrowse = {
				showGallery = false
				photoPickerLauncher.launch(
					PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
				)
			},
			onDismiss = { showGallery = false }
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

@Composable
private fun CaptionField(caption: String, onCaptionChange: (String) -> Unit) {
	val focusRequester = remember { FocusRequester() }
	// opens the keyboard straight away: writing is the point of this page
	LaunchedEffect(Unit) { focusRequester.requestFocus() }

	TextField(
		value = caption,
		onValueChange = onCaptionChange,
		modifier = Modifier
			.fillMaxWidth()
			.focusRequester(focusRequester),
		placeholder = {
			Text(
				text = "What's on your mind?",
				style = MaterialTheme.typography.bodyLarge
			)
		},
		textStyle = MaterialTheme.typography.bodyLarge,
		keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
		colors = TextFieldDefaults.colors(
			focusedContainerColor = Color.Transparent,
			unfocusedContainerColor = Color.Transparent,
			disabledContainerColor = Color.Transparent,
			focusedIndicatorColor = Color.Transparent,
			unfocusedIndicatorColor = Color.Transparent,
			disabledIndicatorColor = Color.Transparent
		)
	)
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
	onGalleryClick: () -> Unit,
	onVideoClick: () -> Unit
) {
	val remaining = MAX_CAPTION_LENGTH - caption.length
	val nearLimit = remaining <= CAPTION_WARNING_THRESHOLD

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 8.dp, vertical = 4.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		IconButton(onClick = onGalleryClick, enabled = canAddMedia) {
			Icon(Icons.Outlined.Image, contentDescription = "Add photos")
		}
		IconButton(onClick = onVideoClick, enabled = canAddMedia) {
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
		Text(
			text = "${caption.length}/$MAX_CAPTION_LENGTH",
			style = MaterialTheme.typography.labelMedium,
			color = if (nearLimit) MaterialTheme.colorScheme.error
			else MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.padding(end = 8.dp)
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GallerySheet(
	images: List<Uri>,
	selectedImages: Set<Uri>,
	selectedCount: Int,
	isLoading: Boolean,
	hasGalleryAccess: Boolean,
	onImageToggled: (Uri) -> Unit,
	onBrowse: () -> Unit,
	onDismiss: () -> Unit
) {
	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.navigationBarsPadding()
				.padding(horizontal = 16.dp)
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						"Recent photos",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.SemiBold
					)
					Text(
						"$selectedCount/$MAX_POST_MEDIA selected",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				OutlinedButton(onClick = onBrowse, shape = RoundedCornerShape(12.dp)) {
					Text("Browse all")
				}
			}
			Spacer(Modifier.height(12.dp))

			when {
				isLoading -> Box(
					Modifier
						.fillMaxWidth()
						.height(160.dp),
					contentAlignment = Alignment.Center
				) {
					CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
				}

				images.isNotEmpty() -> LazyVerticalGrid(
					columns = GridCells.Fixed(3),
					horizontalArrangement = Arrangement.spacedBy(6.dp),
					verticalArrangement = Arrangement.spacedBy(6.dp),
					modifier = Modifier
						.fillMaxWidth()
						.height(360.dp)
				) {
					items(images, key = { it.toString() }) { imageUri ->
						GalleryTile(
							uri = imageUri,
							isSelected = imageUri in selectedImages,
							onClick = { onImageToggled(imageUri) }
						)
					}
				}

				else -> Text(
					text = if (hasGalleryAccess) "No photos in your gallery yet."
					else "Gallery access was not granted. Use Browse all to pick with the system picker.",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(vertical = 24.dp)
				)
			}
			Spacer(Modifier.height(16.dp))
		}
	}
}

@Composable
private fun GalleryTile(uri: Uri, isSelected: Boolean, onClick: () -> Unit) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.aspectRatio(1f)
			.clip(RoundedCornerShape(12.dp))
			.clickable(onClick = onClick)
			.background(MaterialTheme.colorScheme.surfaceVariant)
	) {
		AsyncImage(
			model = uri,
			contentDescription = "Gallery photo",
			modifier = Modifier.fillMaxSize(),
			contentScale = ContentScale.Crop
		)
		if (isSelected) {
			Surface(
				modifier = Modifier
					.align(Alignment.TopEnd)
					.padding(6.dp),
				shape = CircleShape,
				color = MaterialTheme.colorScheme.primary
			) {
				Icon(
					Icons.Default.Check,
					contentDescription = "Selected",
					tint = MaterialTheme.colorScheme.onPrimary,
					modifier = Modifier
						.padding(4.dp)
						.size(14.dp)
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
			onToggleRecentImage = {},
			onRemoveMedia = {},
			onLoadRecentImages = {},
			onClose = {},
			onNext = {}
		)
	}
}
