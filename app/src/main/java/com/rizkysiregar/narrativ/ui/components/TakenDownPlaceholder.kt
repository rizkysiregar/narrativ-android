package com.rizkysiregar.narrativ.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rizkysiregar.narrativ.R
import androidx.compose.ui.res.stringResource

/**
 * Stands in for the media of a post that was taken down by moderation.
 * [compact] drops the text for small grid tiles.
 */
@Composable
fun TakenDownPlaceholder(
	message: String,
	modifier: Modifier = Modifier,
	compact: Boolean = false
) {
	Column(
		modifier = modifier
			.background(MaterialTheme.colorScheme.surfaceVariant)
			.padding(if (compact) 4.dp else 24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
	) {
		Icon(
			imageVector = Icons.Outlined.VisibilityOff,
			contentDescription = stringResource(R.string.cd_post_taken_down),
			tint = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.size(if (compact) 24.dp else 36.dp)
		)
		if (!compact) {
			Text(
				text = stringResource(R.string.post_unavailable),
				style = MaterialTheme.typography.titleSmall,
				color = MaterialTheme.colorScheme.onSurface
			)
			Text(
				text = message,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				textAlign = TextAlign.Center,
				maxLines = 4,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}
