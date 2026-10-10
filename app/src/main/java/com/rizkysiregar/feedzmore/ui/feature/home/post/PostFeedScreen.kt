package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

@Composable
fun PostFeedScreen(
	uiState: PostFeedUiState,
	isPreparing: Boolean,
	onCaptionChange: (String) -> Unit,
	onPickImage: (Uri) -> Unit,
	onLoadRecentImages: () -> Unit,
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

	val photoPickerLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.PickVisualMedia()
	) { uri -> uri?.let(onPickImage) }
	val permissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { granted -> hasGalleryAccess = granted }

	LaunchedEffect(hasGalleryAccess) {
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

	Column(
		modifier = Modifier
			.fillMaxSize()
			.navigationBarsPadding()
			.imePadding()
			.verticalScroll(rememberScrollState())
			.padding(horizontal = 16.dp, vertical = 12.dp),
		verticalArrangement = Arrangement.spacedBy(20.dp)
	) {
		PostHeader()
		SelectedPhotoPreview(
			imageUri = uiState.selectedImage,
			mimeType = uiState.selectedMimeType,
			onChooseAnother = {
				photoPickerLauncher.launch(
					PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
				)
			}
		)
		if (uiState.validationError != null) {
			Text(
				text = uiState.validationError,
				color = MaterialTheme.colorScheme.error,
				style = MaterialTheme.typography.bodyMedium
			)
		}
		RecentPhotosSection(
			images = uiState.recentImages,
			selectedImage = uiState.selectedImage,
			isLoading = uiState.isLoadingGallery,
			hasGalleryAccess = hasGalleryAccess,
			onImageSelected = onPickImage,
			onChooseAnother = {
				photoPickerLauncher.launch(
					PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
				)
			}
		)
		CaptionSection(caption = uiState.caption, onCaptionChange = onCaptionChange)
		if (isPreparing) {
			PreparingSection()
		}
		Button(
			onClick = onNext,
			enabled = uiState.selectedImage != null && !isPreparing,
			modifier = Modifier
				.fillMaxWidth()
				.height(52.dp),
			shape = RoundedCornerShape(16.dp),
			colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
		) {
			if (isPreparing) {
				CircularProgressIndicator(
					modifier = Modifier.size(22.dp),
					strokeWidth = 2.dp,
					color = Color.White
				)
			} else {
				Text("Bagikan feed", fontWeight = FontWeight.SemiBold)
			}
		}
	}
}

@Composable
private fun PreparingSection() {
	Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		Text("Menyiapkan media...", style = MaterialTheme.typography.bodyMedium)
		LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
	}
}

@Composable
private fun PostHeader() {
	Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
		Text("Buat feed baru", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
		Text("Pilih momen terbaikmu, lalu tambahkan cerita singkat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
	}
}

@Composable
private fun SelectedPhotoPreview(
	imageUri: Uri?,
	mimeType: String?,
	onChooseAnother: () -> Unit
) {
	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.aspectRatio(1f)
			.animateContentSize(),
		shape = RoundedCornerShape(24.dp),
		color = MaterialTheme.colorScheme.surfaceVariant
	) {
		Box(contentAlignment = Alignment.Center) {
			AnimatedVisibility(visible = imageUri != null) {
				imageUri?.let { uri ->
					if (mimeType?.startsWith("video/") == true) {
						InlineVideoPreview(uri = uri, active = true)
					} else {
						AsyncImage(
							model = uri,
							contentDescription = "Media yang dipilih",
							modifier = Modifier.fillMaxSize(),
							contentScale = ContentScale.Crop
						)
					}
				}
			}
			if (imageUri == null) {
				Column(horizontalAlignment = Alignment.CenterHorizontally) {
					Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(36.dp))
					Spacer(Modifier.height(8.dp))
					Text("Pilih foto atau video untuk memulai")
				}
			}
			OutlinedButton(
				onClick = onChooseAnother,
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(12.dp),
				shape = RoundedCornerShape(14.dp),
				colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.92f))
			) {
				Icon(Icons.Default.Add, contentDescription = null)
				Spacer(Modifier.width(6.dp))
				Text(if (imageUri == null) "Pilih foto" else "Ganti")
			}
		}
	}
}

@Composable
private fun InlineVideoPreview(uri: Uri, active: Boolean) {
	val context = LocalContext.current
	val player = remember {
		ExoPlayer.Builder(context).build().apply {
			setMediaItem(MediaItem.fromUri(uri))
			prepare()
			playWhenReady = true
		}
	}
	DisposableEffect(Unit) {
		onDispose { player.release() }
	}
	LaunchedEffect(active) {
		if (active) player.play()
		else player.pause()
	}
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surfaceVariant),
		contentAlignment = Alignment.Center
	) {
		AndroidView(
			factory = { ctx ->
				PlayerView(ctx).apply {
					this.player = player
					useController = true
					resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
				}
			},
			modifier = Modifier.fillMaxSize()
		)
	}
}

@Composable
private fun RecentPhotosSection(
	images: List<Uri>,
	selectedImage: Uri?,
	isLoading: Boolean,
	hasGalleryAccess: Boolean,
	onImageSelected: (Uri) -> Unit,
	onChooseAnother: () -> Unit
) {
	Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Column(modifier = Modifier.weight(1f)) {
				Text("Foto terbaru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
				Text("Pilih satu untuk dibagikan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
			OutlinedButton(onClick = onChooseAnother, shape = RoundedCornerShape(12.dp)) {
				Text("Lainnya")
			}
		}

		when {
			isLoading -> Box(Modifier.fillMaxWidth().height(104.dp), contentAlignment = Alignment.Center) {
				CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
			}

			images.isNotEmpty() -> LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
				items(images, key = { it.toString() }) { imageUri ->
					RecentPhotoTile(
						uri = imageUri,
						isSelected = imageUri == selectedImage,
						onClick = { onImageSelected(imageUri) }
					)
				}
			}

			else -> Text(
				text = if (hasGalleryAccess) "Belum ada foto di gallery." else "Izin gallery tidak diberikan. Pilih foto melalui pemilih sistem.",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
private fun RecentPhotoTile(uri: Uri, isSelected: Boolean, onClick: () -> Unit) {
	val scale by animateFloatAsState(
		targetValue = if (isSelected) 1f else 0.94f,
		animationSpec = tween(180),
		label = "gallery-photo-scale"
	)
	Box(
		modifier = Modifier
			.size(88.dp)
			.scale(scale)
			.clip(RoundedCornerShape(16.dp))
			.clickable(onClick = onClick)
			.background(MaterialTheme.colorScheme.surfaceVariant)
	) {
		AsyncImage(
			model = uri,
			contentDescription = "Foto gallery",
			modifier = Modifier.fillMaxSize(),
			contentScale = ContentScale.Crop
		)
		if (isSelected) {
			Surface(
				modifier = Modifier
					.align(Alignment.TopEnd)
					.padding(6.dp),
				shape = CircleShape,
				color = Color.Black
			) {
				Icon(
					Icons.Default.Check,
					contentDescription = "Foto terpilih",
					tint = Color.White,
					modifier = Modifier.padding(4.dp).size(14.dp)
				)
			}
		}
	}
}

@Composable
private fun CaptionSection(caption: String, onCaptionChange: (String) -> Unit) {
	Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		Text("Caption", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
		OutlinedTextField(
			value = caption,
			onValueChange = onCaptionChange,
			placeholder = { Text("Ceritakan tentang post ini...") },
			modifier = Modifier
				.fillMaxWidth()
				.heightIn(min = 120.dp),
			maxLines = 5,
			shape = RoundedCornerShape(18.dp)
		)
		Text(
			text = "${caption.length}/2200",
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.align(Alignment.End)
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun PostFeedScreenPreview() {
	FeedzmoreTheme {
		PostFeedScreen(
			uiState = PostFeedUiState(selectedImage = "https://example.com/image.jpg".toUri()),
			isPreparing = false,
			onCaptionChange = {},
			onPickImage = {},
			onLoadRecentImages = {},
			onNext = {}
		)
	}
}
