package com.rizkysiregar.feedzmore.ui.feature.home.post

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.rizkysiregar.feedzmore.ui.theme.FeedzmoreTheme

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 07/12/25
 * Description: Software Engineer
 */


@Composable
fun PostFeedScreen(
	uiState: PostFeedUiState,
	onCaptionChange: (String) -> Unit,
	onPickImage: (Uri) -> Unit,
	onNext: () -> Unit
) {
	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.PickVisualMedia()
	) { uri ->
		if (uri != null) {
			Log.i("PhotoPicker", "Selected URI: $uri")
			onPickImage(uri)
		} else {
			Log.i("PhotoPicker", "No media selected")
		}
	}


	Column(
		modifier = Modifier
			.fillMaxSize()
			.navigationBarsPadding()
			.imePadding()
			.verticalScroll(rememberScrollState())
	) {

		// -- Preview -- //
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(1f)
				.background(Color.Black)
		) {
			uiState.selectedImage?.let { imageUri ->
				AsyncImage(
					model = imageUri,
					contentDescription = null,
					modifier = Modifier.fillMaxSize(),
					contentScale = ContentScale.Crop
				)
			}

			// -- Select Photo FAB -- //
			FloatingActionButton(
				onClick = {
					launcher.launch(
						PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
					)
				},
				containerColor = Color.White.copy(alpha = 0.9f),
				shape = CircleShape,
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(8.dp)
			) {
				Icon(
					imageVector = Icons.Default.Add,
					contentDescription = "Pick Image",
					tint = Color.Black
				)
			}
		}

		Spacer(Modifier.height(16.dp))

		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(16.dp)
		) {

			Text(
				text = "Write a caption",
				style = MaterialTheme.typography.titleMedium
			)

			Spacer(Modifier.height(12.dp))

			// Caption text field
			OutlinedTextField(
				value = uiState.caption,
				onValueChange = onCaptionChange,
				placeholder = {
					Text("Write something...")
				},
				modifier = Modifier
					.fillMaxWidth()
					.heightIn(min = 120.dp),
				maxLines = 10,
				shape = RoundedCornerShape(12.dp)
			)

			Spacer(Modifier.height(8.dp))

			// Character counter (optional)
			Text(
				text = "${uiState.caption.length}/2200",
				style = MaterialTheme.typography.labelSmall,
				color = Color.Gray,
				modifier = Modifier.align(Alignment.End)
			)

			Spacer(Modifier.weight(1f))

			Button(
				onClick = onNext,
				enabled = uiState.selectedImage != null,
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp)
			) {
				Text("Next")
			}
		}

	}

}


@Preview
@Composable
private fun PostImagePickerScreenPreview() {
	FeedzmoreTheme {
		PostFeedScreen(
			uiState = PostFeedUiState(selectedImage = "https://example.com/image.jpg".toUri()),
			onCaptionChange = {},
			onPickImage = {},
			onNext = {}
		)
	}
}