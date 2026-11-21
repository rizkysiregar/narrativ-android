package com.rizkysiregar.narrativ.ui.components

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rizkysiregar.narrativ.R
import androidx.compose.ui.res.stringResource

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 11/01/26
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
			contentDescription = stringResource(R.string.cd_avatar_placeholder),
			modifier = Modifier
				.size(size)
				.clip(CircleShape)

		)
		return
	}

	when {
		!imageUrl.isNullOrBlank() -> {
			// show real profile picture
			AsyncImage(
				model = rememberAvatarRequest(imageUrl),
				contentDescription = stringResource(R.string.cd_profile_photo),
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
				contentDescription = stringResource(R.string.cd_avatar),
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


/**
 * The request for a profile picture. A presigned url changes on every response, so it is cached on the url without
 * its signature.
 */
@Composable
fun rememberAvatarRequest(imageUrl: String, crossfade: Boolean = false): ImageRequest {
	val context = LocalPlatformContext.current
	val cacheKey = imageUrl.substringBefore('?')
	return remember(imageUrl, crossfade) {
		ImageRequest.Builder(context)
			.data(imageUrl)
			.memoryCacheKey(cacheKey)
			.diskCacheKey(cacheKey)
			.crossfade(crossfade)
			.build()
	}
}

/**
 * The avatar of the author of a post: their photo when they have one, else their initials (what posts always showed).
 * The initials sit under the photo, so while it loads, or when it fails to load, they stay in view instead of a hole,
 * and the photo fades in over them.
 */
@Composable
fun PostAuthorAvatar(
	imageUrl: String?,
	name: String,
	size: Dp,
	modifier: Modifier = Modifier
) {
	Box(modifier = modifier.size(size).clip(CircleShape)) {
		InitialAvatar(name = name, size = size)
		if (!imageUrl.isNullOrBlank()) {
			AsyncImage(
				model = rememberAvatarRequest(imageUrl, crossfade = true),
				contentDescription = stringResource(R.string.cd_profile_photo),
				contentScale = ContentScale.Crop,
				modifier = Modifier.matchParentSize()
			)
		}
	}
}

@Composable
fun InitialAvatar(
	name: String,
	size: Dp,
	modifier: Modifier = Modifier
) {
	val initial = name
		.trim()
		.split(" ")
		.mapNotNull { it.firstOrNull()?.uppercase() }
		.joinToString("")
		.take(2)

	Box(
		contentAlignment = Alignment.Center,
		modifier = modifier
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



