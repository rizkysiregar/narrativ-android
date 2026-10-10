package com.rizkysiregar.feedzmore.ui.feature.home.upload

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Top progress banner shown on Home while a post upload runs in the
 * background. Disappears once all uploads finish.
 */
@Composable
fun UploadProgressBanner(
	viewModel: UploadBannerViewModel
) {
	val pendingUploads by viewModel.pendingUploads.collectAsStateWithLifecycle(initialValue = emptyList())
	val hasActive = pendingUploads.any { !it.state.isFinished }

	AnimatedVisibility(
		visible = hasActive,
		enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
		exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
	) {
		Surface(
			modifier = Modifier.fillMaxWidth(),
			shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
			color = MaterialTheme.colorScheme.surfaceVariant
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 12.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				val first = pendingUploads.minByOrNull { it.progress } ?: return@Row
				CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = "Mengunggah post",
						style = MaterialTheme.typography.titleSmall
					)
					Spacer(Modifier.height(2.dp))
					Text(
						text = first.stageLabel.ifBlank { "Menunggu koneksi..." },
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						maxLines = 1
					)
					Spacer(Modifier.height(6.dp))
					LinearProgressIndicator(
						progress = { first.progress / 100f },
						modifier = Modifier.fillMaxWidth()
					)
				}
				Icon(
					imageVector = Icons.Default.Close,
					contentDescription = "Batalkan upload",
					modifier = Modifier
						.size(20.dp)
						.clip(RoundedCornerShape(10.dp))
						.clickable { viewModel.cancelUpload(first.id) }
				)
			}
		}
	}
}