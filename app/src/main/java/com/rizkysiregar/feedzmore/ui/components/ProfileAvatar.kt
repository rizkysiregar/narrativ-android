package com.rizkysiregar.feedzmore.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.rizkysiregar.feedzmore.R

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 11/01/26
 * Description: Software Engineer
 */


@Composable
fun ProfileAvatar(
	imageUrl: String?,  // real profile picture
	userId: String,     // stable id for DiceBear seed
	userName: String,   // used for initial fallback
	size: Dp = 72.dp
) {

	// check for preview mode
	if (LocalInspectionMode.current && (!imageUrl.isNullOrBlank() || userId.isNotBlank())) {
		Image(
			painter = painterResource(id = R.drawable.avatar),
			contentDescription = "Preview PlaceHolder",
			modifier = Modifier
				.size(size)
				.clip(CircleShape)

		)
		return
	}

	when {
		!imageUrl.isNullOrBlank() -> {
			// show real profile picture
			// a presigned url changes on every response, so cache on the url without its signature
			val context = LocalPlatformContext.current
			val cacheKey = imageUrl.substringBefore('?')
			val request = remember(imageUrl) {
				ImageRequest.Builder(context)
					.data(imageUrl)
					.memoryCacheKey(cacheKey)
					.diskCacheKey(cacheKey)
					.build()
			}
			AsyncImage(
				model = request,
				contentDescription = "Profile Photo",
				modifier = Modifier
					.size(size)
					.clip(CircleShape)
			)
		}

		userId.isNotBlank() -> {
			// DIceBear avatar fallback
			val diceBearUrl = "https://api.dicebear.com/6.x/avataaars/png?seed=$userId"
			AsyncImage(
				model = diceBearUrl,
				contentDescription = "Avatar",
				modifier = Modifier
					.size(size)
					.clip(CircleShape)
			)
		}

		else -> {
			// Initial fallback
			InitialAvatar(
				name = userName,
				size = size
			)

		}
	}
}


@Composable
fun InitialAvatar(
	name: String,
	size: Dp
) {
	val initial = name
		.trim()
		.split(" ")
		.mapNotNull { it.firstOrNull()?.uppercase() }
		.joinToString("")
		.take(2)

	Box(
		contentAlignment = Alignment.Center,
		modifier = Modifier
			.size(size)
			.clip(CircleShape)
			.background(MaterialTheme.colorScheme.primary)
	) {
		Text(
			text = initial.ifEmpty { "?" },
			color = MaterialTheme.colorScheme.onBackground,
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.Bold
		)
	}
}


@Preview(name = "With Photo", showBackground = true)
@Composable
fun ProfileAvatarWithPhotoPreview() {
	ProfileAvatar(
		imageUrl = "https://i.pravatar.cc/300",
		userId = "user123",
		userName = "Habib Rizky",
		size = 96.dp
	)
}

@Preview(name = "DiceBear Avatar", showBackground = true)
@Composable
fun ProfileAvatarDiceBearPreview() {
	ProfileAvatar(
		imageUrl = null,
		userId = "123123",
		userName = "Habib Rizky",
		size = 96.dp
	)
}

@Preview(name = "Initials Only", showBackground = true)
@Composable
fun ProfileAvatarInitialsPreview() {
	ProfileAvatar(
		imageUrl = null,
		userId = "",
		userName = "Habib Rizky",
		size = 96.dp
	)
}



