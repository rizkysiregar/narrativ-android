package com.rizkysiregar.feedzmore.ui.feature.home.profile

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.rizkysiregar.feedzmore.media.AvatarFilter
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rizkysiregar.feedzmore.media.AvatarCropper
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.min
import com.rizkysiregar.feedzmore.R
import androidx.compose.ui.res.stringResource

private const val MAX_ZOOM = 5f

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 11/11/26
 * Description: Full-screen crop step for a profile photo. The image is dragged and pinched under a
 * fixed circular window and the visible square is returned as a JPEG file (the app shows it as a circle).
 */
@Composable
fun AvatarCropScreen(
	uri: Uri,
	onCancel: () -> Unit,
	onCropped: (File) -> Unit,
	onLoadFailed: () -> Unit
) {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	// null while decoding; Loaded(null) when the file could not be read
	val loaded by produceState<Loaded?>(null, uri) { value = Loaded(AvatarCropper.load(context, uri)) }
	val bitmap = loaded?.bitmap
	LaunchedEffect(loaded) { if (loaded != null && bitmap == null) onLoadFailed() }
	var containerSize by remember { mutableStateOf(IntSize.Zero) }
	var zoom by remember { mutableFloatStateOf(1f) }
	var offset by remember { mutableStateOf(Offset.Zero) }
	var isCropping by remember { mutableStateOf(false) }
	var filter by remember { mutableStateOf(AvatarFilter.ORIGINAL) }

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color.Black)
			.onSizeChanged { containerSize = it }
	) {
		val image = bitmap
		if (image == null) {
			CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
			return@Box
		}
		if (containerSize == IntSize.Zero) return@Box

		val diameter = min(containerSize.width, containerSize.height) - with(LocalDensity.current) { 32.dp.toPx() }
		val minScale = max(diameter / image.width, diameter / image.height)
		val scale = minScale * zoom

		fun clamp(value: Offset, forScale: Float): Offset {
			val maxX = max(0f, (image.width * forScale - diameter) / 2f)
			val maxY = max(0f, (image.height * forScale - diameter) / 2f)
			return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
		}

		Image(
			bitmap = remember(image) { image.asImageBitmap() },
			contentDescription = null,
			colorFilter = filterOf(filter),
			modifier = Modifier
				.fillMaxSize()
				.pointerInput(image, diameter) {
					detectTransformGestures { centroid, pan, gestureZoom, _ ->
						val newZoom = (zoom * gestureZoom).coerceIn(1f, MAX_ZOOM)
						val ratio = newZoom / zoom
						val pivot = centroid - Offset(containerSize.width / 2f, containerSize.height / 2f)
						zoom = newZoom
						offset = clamp(pivot + (offset - pivot) * ratio + pan, minScale * newZoom)
					}
				}
				.graphicsLayer {
					// drawn at its natural size, centred, then scaled and shifted
					scaleX = scale
					scaleY = scale
					translationX = offset.x
					translationY = offset.y
				},
			contentScale = ContentScale.None
		)

		// dimmed surround with a circular window
		Canvas(
			modifier = Modifier
				.fillMaxSize()
				.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
		) {
			val center = Offset(size.width / 2f, size.height / 2f)
			drawRect(Color.Black.copy(alpha = 0.6f), size = Size(size.width, size.height))
			drawCircle(Color.Black, radius = diameter / 2f, center = center, blendMode = BlendMode.Clear)
			drawCircle(Color.White, radius = diameter / 2f, center = center, style = Stroke(width = 2.dp.toPx()))
		}

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.statusBarsPadding()
				.padding(horizontal = 8.dp)
				.align(Alignment.TopCenter),
			verticalAlignment = Alignment.CenterVertically
		) {
			TextButton(onClick = onCancel, enabled = !isCropping) {
				Text(stringResource(R.string.cancel), color = Color.White)
			}
			Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
				Text(stringResource(R.string.adjust_photo), color = Color.White)
			}
			TextButton(
				enabled = !isCropping,
				onClick = {
					isCropping = true
					// the window's top-left in bitmap pixels
					val size = diameter / scale
					val left = image.width / 2f - offset.x / scale - size / 2f
					val top = image.height / 2f - offset.y / scale - size / 2f
					scope.launch {
						val file = AvatarCropper.crop(context, image, left, top, size, filter)
						onCropped(file)
					}
				}
			) {
				Text(stringResource(R.string.choose), color = Color.White)
			}
		}

		Column(
			modifier = Modifier
				.align(Alignment.BottomCenter)
				.fillMaxWidth()
				.navigationBarsPadding()
				.padding(bottom = 16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = stringResource(R.string.crop_hint),
				color = Color.White.copy(alpha = 0.8f),
				modifier = Modifier.padding(bottom = 16.dp)
			)
			FilterStrip(image = image, selected = filter, onSelect = { filter = it })
		}
	}

}

private fun filterOf(filter: AvatarFilter): ColorFilter? =
	if (filter.isOriginal) null else ColorFilter.colorMatrix(ColorMatrix(filter.matrix))

@Composable
private fun FilterStrip(image: Bitmap, selected: AvatarFilter, onSelect: (AvatarFilter) -> Unit) {
	// a small centred square of the photo, so every preset previews quickly
	val thumb = remember(image) {
		val side = min(image.width, image.height)
		val square = Bitmap.createBitmap(image, (image.width - side) / 2, (image.height - side) / 2, side, side)
		Bitmap.createScaledBitmap(square, 160, 160, true).asImageBitmap()
	}
	LazyRow(
		contentPadding = PaddingValues(horizontal = 16.dp),
		horizontalArrangement = Arrangement.spacedBy(12.dp)
	) {
		items(AvatarFilter.entries) { option ->
			val isSelected = option == selected
			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				modifier = Modifier.clickable { onSelect(option) }
			) {
				Image(
					bitmap = thumb,
					contentDescription = stringResource(option.labelRes),
					colorFilter = filterOf(option),
					contentScale = ContentScale.Crop,
					modifier = Modifier
						.size(64.dp)
						.clip(RoundedCornerShape(8.dp))
						.border(
							width = if (isSelected) 2.dp else 0.dp,
							color = if (isSelected) Color.White else Color.Transparent,
							shape = RoundedCornerShape(8.dp)
						)
				)
				Text(
					text = stringResource(option.labelRes),
					color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
					style = MaterialTheme.typography.labelSmall,
					modifier = Modifier.padding(top = 4.dp)
				)
			}
		}
	}
}

private class Loaded(val bitmap: Bitmap?)
